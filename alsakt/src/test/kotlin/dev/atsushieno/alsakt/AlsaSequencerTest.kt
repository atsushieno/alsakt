package dev.atsushieno.alsakt

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

val isAlsaAvailable = with(File("/dev/snd/seq")) { this.exists() && this.canRead() }

class AlsaSequencerTest {
    @Test
    fun simpleUse() {
        if (!isAlsaAvailable) return

        val seq = AlsaSequencer(AlsaIOType.Duplex, AlsaIOMode.NonBlocking)
        seq.close()
    }

    @Test
    fun systemInfo() {
        if (!isAlsaAvailable) return

        AlsaSequencer(AlsaIOType.Output, AlsaIOMode.NonBlocking).use { seq ->
            AlsaSystemInfo().use { sys ->
                sys.setContextSequencer(seq)
                // FIXME: investigate this (so far it's failing, but not sure if test is alright)
                //assertEquals(1, sys.currentQueueCount, "cur_q")
                assertTrue(0 < sys.currentClientCount, "cur_ch")
                assertTrue(0 < sys.portCount, "port")
                assertTrue(0 < sys.channelCount, "ch")
                assertTrue(sys.currentQueueCount < sys.maxQueueCount, "max_q")
                assertTrue(sys.currentClientCount < sys.maxClientCount, "max_cli")
                sys
            }
            seq
        }
    }

    @Test
    fun getClient() {
        if (!isAlsaAvailable) return

        AlsaSequencer (AlsaIOType.Input, AlsaIOMode.NonBlocking).use { seq ->
            seq.getClient (AlsaSequencer.ClientSystem).use { cli ->
                println(cli.name)
                println(cli.card)
                println(cli.client)
                println(cli.portCount)
                println(cli.broadcastFilter)
                println(cli.midiVersion)
            }
        }
    }

    @Test
    fun getPort() {
        if (!isAlsaAvailable) return

        AlsaSequencer(AlsaIOType.Input, AlsaIOMode.NonBlocking).use { seq ->
            seq.getPort(AlsaSequencer.ClientSystem, AlsaPortInfo.PortSystemAnnouncement).use { port ->
                println(port.id)
                println(port.name)
            }
        }
    }

    @Test
    fun setClientName() {
        if (!isAlsaAvailable) return

        AlsaSequencer (AlsaIOType.Input, AlsaIOMode.NonBlocking, "default").use { seq ->
            assertEquals ("default", seq.name, "#1")
            seq.setClientName ("overwritten sequencer name")
            assertEquals("default", seq.name, "#2")
            assertEquals ("overwritten sequencer name", seq.getClient (seq.currentClientId).name, "#3")
        }
    }

    @Test
    fun setClientNameInvalid() {
        if (!isAlsaAvailable) return

        assertThrows<AlsaException> {
            AlsaSequencer (AlsaIOType.Input, AlsaIOMode.NonBlocking, "this_is_an_invalid_config_name_and_should_throw_exception")
        }
    }

    @Test
    fun enumerateClientAndPortsPrimitive() {
        if (!isAlsaAvailable) return

        AlsaSequencer(AlsaIOType.Output, AlsaIOMode.NonBlocking).use { seq ->
            val cli = AlsaClientInfo()
            cli.client = -1
            while (seq.queryNextClient(cli)) {
                println("Client:" + cli.client)
                println(cli.name)
                println(cli.card)
                println(cli.client)
                println(cli.portCount)
                println(cli.broadcastFilter)

                val port = AlsaPortInfo()
                port.client = cli.client
                port.port = -1
                while (seq.queryNextPort(port)) {
                    println("  Port:" + port.id)
                    println(port.id)
                    println(port.name)
                }
            }
        }
    }

    @Test
    fun subscribeUnsubscribePort() {
        if (!isAlsaAvailable) return

        AlsaSequencer (AlsaIOType.Input, AlsaIOMode.NonBlocking).use { seq ->
            val subs = AlsaPortSubscription ()
            subs.sender.client = AlsaSequencer.ClientSystem.toByte()
            subs.sender.port = AlsaPortInfo.PortSystemAnnouncement.toByte()
            subs.destination.client = seq.currentClientId.toByte()
            subs.destination.port = seq.createSimplePort ("test in port", AlsaPortCapabilities.SubsRead or AlsaPortCapabilities.Read, AlsaPortType.MidiGeneric or AlsaPortType.Application).toByte()
            try {
                seq.subscribePort (subs)
                println(subs.sender)
                println(subs.destination)
                println(subs.exclusive)
                println(subs.isRealTimeUpdateMode)
                seq.unsubscribePort (subs)
            } finally {
                seq.deleteSimplePort (subs.destination.port.toInt())
            }
        }
    }

    // FIXME: this test assumes that there is a Timidity alsaseq instance...
    @Test
    fun send() {
        if (!isAlsaAvailable) return

        AlsaSequencer (AlsaIOType.Output, AlsaIOMode.NonBlocking).use { seq ->
            val cinfo = AlsaClientInfo().apply { client = -1 }
            var lastClient = -1
            while (seq.queryNextClient (cinfo))
                if (cinfo.name.contains ("TiMidity"))
                    lastClient = cinfo.client
            if (lastClient < 0) {
                println ("TiMidity not found. Not testable.")
                return // not testable
            }

            var targetPort = 3
            try {
                seq.getPort (lastClient, targetPort)
            } catch(ex: AlsaException) {
                println ("TiMidity port #3 not available. Not testable.")
                return // not testable
            }

            val appPort = seq.createSimplePort ("alsa-sharp-test-output", AlsaPortCapabilities.Write or AlsaPortCapabilities.NoExport, AlsaPortType.Application or AlsaPortType.MidiGeneric)
            try {
                seq.connectDestination (appPort, lastClient, targetPort)
                val setup = byteArrayOf(0xC0.toByte(), 0x48, 0xB0.toByte(), 7, 110, 0xB0.toByte(), 11, 127)
                val keyon = byteArrayOf( 0x90.toByte(), 0x40, 0x70 )
                val keyoff = byteArrayOf( 0x80.toByte(), 0x40, 0x70 )
                seq.send (appPort, setup, 0, setup.size)
                seq.send (appPort, keyon, 0, keyon.size)
                runBlocking {
                    //delay(100)
                    seq.send(appPort, keyoff, 0, keyoff.size)
                    //delay(100)
                }
                seq.disconnectDestination (appPort, lastClient, targetPort)
            } finally {
                seq.deleteSimplePort (appPort)
            }
        }
    }

    @Test
    fun inputToObserveSystemAnnouncements() {
        if (!isAlsaAvailable) return

        var passed = false
        val evt = AlsaSequencerEvent ()
        var appPort = - 1
        val task = GlobalScope.launch {
            AlsaSequencer (AlsaIOType.Input, AlsaIOMode.None).use { inseq ->
                appPort = inseq.createSimplePort(
                    "alsa-sharp-test-input",
                    AlsaPortCapabilities.Write or AlsaPortCapabilities . NoExport, AlsaPortType.Application or AlsaPortType.MidiGeneric)
                inseq.connectSource(appPort, AlsaSequencer.ClientSystem, AlsaPortInfo.PortSystemAnnouncement)
                try {
                    inseq.resetPoolInput()
                    // ClientStart, PortStart, PortSubscribed, PortUnsubscribed, PortExit
                    inseq.input(evt, appPort)
                    assertEquals(AlsaSequencerEventType.ClientStart, evt.eventType, "evt1")
                    inseq.input(evt, appPort)
                    assertEquals(AlsaSequencerEventType.PortStart, evt.eventType, "evt2")
                    inseq.input(evt, appPort)
                    assertEquals(AlsaSequencerEventType.PortSubscribed, evt.eventType, "evt3")
                    passed = true
                } finally {
                    inseq.disconnectSource(appPort, AlsaSequencer.ClientSystem, AlsaPortInfo.PortSystemAnnouncement)
                    appPort = -1
                }
            }
        }
        runBlocking {
            delay(50) // give some time for announcement client to start.
        }
        // create another port, which is a dummy and just subscribes to notify the system to raise an announcement event.
        val cInfo =  AlsaClientInfo().apply { client = -1 }
        var lastClient = - 1
        val outSeq =  AlsaSequencer (AlsaIOType.Input, AlsaIOMode.NonBlocking)
        while (outSeq.queryNextClient(cInfo))
            if (cInfo.name.contains("Midi Through"))
                lastClient = cInfo.client
        if (lastClient < 0) {
            println("Midi Through not found. Not testable.")
            return // not testable
        }
        val targetPort = 0

        val testPort = outSeq . createSimplePort ("alsa-sharp-test-output", AlsaPortCapabilities.Write or AlsaPortCapabilities.NoExport, AlsaPortType.Application or AlsaPortType.MidiGeneric)
        try {
            outSeq.connectDestination(testPort, lastClient, targetPort)
            outSeq.disconnectDestination(testPort, lastClient, targetPort)
            runBlocking {
                delay(50) // give some time for announcement client to finish.
            }
            assertTrue(passed, "failed to receive an announcement")
        } finally {
            outSeq.deleteSimplePort(testPort)
        }
    }

    // FIXME: this test assumes that Keystation is connected and available, and that user can control it
    //  (and that user notices the test console output...)
    @Test
    fun receive() {
        if (!isAlsaAvailable) return

        AlsaSequencer (AlsaIOType.Input, AlsaIOMode.None).use { seq ->
            val cInfo = AlsaClientInfo().apply { client = -1 }
            var lastClient = -1
            while (seq.queryNextClient(cInfo))
                if (cInfo.name.contains("Keystation"))
                    lastClient = cInfo.client
            if (lastClient < 0) {
                println("Keystation not found. Not testable.")
                return // not testable
            }
            println("Press any key on Keystation to continue...")

            val targetPort = 0

            val appPort = seq.createSimplePort(
                "alsa-sharp-test-input",
                AlsaPortCapabilities.Write or AlsaPortCapabilities.NoExport,
                AlsaPortType.Application or AlsaPortType.MidiGeneric
            )
            try {
                seq.connectSource(appPort, lastClient, targetPort)
                var data = byteArrayOf(0, 0, 0)
                var received = seq.receive(appPort, data, 0, 3)
                assertEquals(3, received, "received size")
                assertEquals(0x90, data[0], "received status")
                seq.disconnectSource(appPort, lastClient, targetPort)
            } finally {
                seq.deleteSimplePort(appPort)
            }
        }
    }

    @Test
    fun interruptInput () {
        if (!isAlsaAvailable) return

        var passed = false
        var aborted = false
        val evt = AlsaSequencerEvent ()
        var appPort = -1
        val waitStart = Semaphore(1, 0)
        AlsaSequencer (AlsaIOType.Input, AlsaIOMode.None).use { seq ->
            appPort = seq.createSimplePort ("alsa-sharp-test-input", AlsaPortCapabilities.Write or AlsaPortCapabilities.NoExport, AlsaPortType.Application or AlsaPortType.MidiGeneric)
            val cInfo = AlsaClientInfo().apply { client = -1 }
            var client = -1
            while (seq.queryNextClient (cInfo))
                if (cInfo.name.contains ("Midi Through"))
                    client = cInfo.client
            if (client < 0) {
                println ("Midi Through not found. Not testable.")
                return // not testable
            }
            seq.connectSource (appPort, client, 0)
            seq.resetPoolInput ()
            var job = GlobalScope.launch {
                delay(50)
                if (waitStart.availablePermits == 0)
                    waitStart.release ()
                try {
                    seq.input (evt, appPort)
                    passed = true
                } catch (ex:Exception) {
                    passed = false
                    aborted = true
                    println ("Input threw an error: " + ex)
                }
            }
            runBlocking {
                waitStart.acquire()
                // it will cause ALSA error as it's waiting for input in blocking mode.
                seq.disconnectSource(appPort, client, 0)
                delay(50)
                assertFalse(passed, "input should keep listening")
                seq.deleteSimplePort(appPort)
                delay(50)
            }
        }
        // It doesn't even run exception part...
        assertFalse (aborted, "We expect it not to abort...")
    }

    // FIXME: this test assumes that Keystation or Seaboard is connected and available, and that user can control it
    //  (and that user notices the test console output...)
    @Test
    fun listening() {
        if (!isAlsaAvailable) return

        AlsaSequencer (AlsaIOType.Input, AlsaIOMode.NonBlocking).use { seq ->
            val cInfo = AlsaClientInfo().apply { client = -1 }
            var lastClient = -1
            while (seq.queryNextClient (cInfo))
                if (cInfo.name.contains ("Keystation") || cInfo.name.contains ("Seaboard"))
                    lastClient = cInfo.client
            if (lastClient < 0) {
                println ("Supported devices not found. Not testable.")
                return // not testable
            }
            println ("Press any MIDI key to continue...")

            val targetPort = 0

            val appPort = seq.createSimplePort ("alsa-sharp-test-input", AlsaPortCapabilities.Write or AlsaPortCapabilities.NoExport, AlsaPortType.Application or AlsaPortType.MidiGeneric)
            try {
                seq.connectSource (appPort, lastClient, targetPort)
                val data = byteArrayOf(0, 0, 0)
                var cbData:ByteArray? = null
                val wait = Semaphore (1, 0)
                var cbStart = -1
                var cbLen = -1
                seq.startListening (appPort, data, { cbd, start, len ->
                    cbData = cbd
                    cbStart = start
                    cbLen = len
                    wait.release ()
                }, 60000)
                runBlocking {
                    wait.acquire ()
                    seq.stopListening ()
                    assertNotNull (cbData, "received data")
                    assertEquals (0, cbStart, "received start")
                    assertEquals  (3, cbLen, "received size")
                    seq.disconnectSource (appPort, lastClient, targetPort)
                }
            } finally {
                seq.deleteSimplePort (appPort)
            }
        }
    }
}
