package dev.atsushieno.alsakt
import dev.atsushieno.alsakt.javacpp.global.Alsa
import dev.atsushieno.alsakt.javacpp.global.HackyPoll
import dev.atsushieno.alsakt.javacpp.pollfd
import dev.atsushieno.alsakt.javacpp.snd_midi_event_t
import dev.atsushieno.alsakt.javacpp.snd_seq_addr_t
import dev.atsushieno.alsakt.javacpp.snd_seq_event_t
import dev.atsushieno.alsakt.javacpp.snd_seq_t
import dev.atsushieno.alsakt.javacpp.snd_seq_timestamp_t
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacpp.Loader.sizeof
import java.nio.ByteBuffer

@Suppress("unused")
class AlsaSequencer(
    val ioType: Int, ioMode: Int,
    val driverName: String = "default"
) : AutoCloseable {

    private val seq: snd_seq_t
    private var driverNameHandle : BytePointer? = null

    internal val sequencerHandle : snd_seq_t?
        get() = seq

    override fun close() {
        if (midiEventParserOutput != null) {
            Alsa.snd_midi_event_free(midiEventParserOutput)
            midiEventParserOutput = null
        }
        driverNameHandle?.deallocate()
        driverNameHandle = null
        portNameHandle?.deallocate()
        portNameHandle = null
        nameHandle?.deallocate()
        nameHandle = null
        if (seq != null)
            Alsa.snd_seq_close(seq)
    }

    val name :String
        get() = Alsa.snd_seq_name(seq).string

    val sequencerType: Int
        get() = Alsa.snd_seq_type(seq)

    fun setNonBlockingMode(toNonBlockingMode: Boolean) {
        Alsa.snd_seq_nonblock(seq, if (toNonBlockingMode) 1 else 0)
    }

    val currentClientId: Int
        get() = Alsa.snd_seq_client_id(seq)

    var inputBufferSize: Long
        get() = Alsa.snd_seq_get_input_buffer_size(seq)
        set(value) { Alsa.snd_seq_set_input_buffer_size(seq, value) }

    var outputBufferSize: Long
        get() = Alsa.snd_seq_get_output_buffer_size(seq)
        set(value) { Alsa.snd_seq_set_output_buffer_size(seq, value) }

    val targetPortType: Int
        get() = AlsaPortType.MidiGeneric or AlsaPortType.Synth or AlsaPortType.Application

    fun queryNextClient(client: AlsaClientInfo): Boolean {
        val ret = Alsa.snd_seq_query_next_client(seq, client.handle)
        return ret >= 0
    }

    fun queryNextPort(port: AlsaPortInfo): Boolean {
        val ret = Alsa.snd_seq_query_next_port(seq, port.handle)
        return ret >= 0
    }

    fun getClient(client: Int): AlsaClientInfo {
        val info = AlsaClientInfo()
        val ret = Alsa.snd_seq_get_any_client_info(seq, client, info.handle)
        if (ret != 0)
            throw AlsaException(ret)
        return info
    }

    fun getPort(client: Int, port: Int): AlsaPortInfo {
        val info = AlsaPortInfo()
        val err = Alsa.snd_seq_get_any_port_info(seq, client, port, info.handle)
        if (err != 0)
            throw AlsaException(err)
        return info
    }

    private var portNameHandle : BytePointer? = null

    fun createSimplePort(name: String?, caps: Int, type: Int): Int {
        portNameHandle?.deallocate()
        portNameHandle = if (name == null) null else BytePointer(name)
        return Alsa.snd_seq_create_simple_port(seq, portNameHandle, caps, type)
    }

    fun deleteSimplePort(port: Int) {
        val ret = Alsa.snd_seq_delete_simple_port(seq, port)
        if (ret != 0)
            throw AlsaException(ret)
    }

    private var nameHandle: BytePointer? = null

    fun setClientName(name: String) {
        if (name == null)
            throw IllegalArgumentException("name is null")

        nameHandle?.deallocate()
        nameHandle = BytePointer(name)
        Alsa.snd_seq_set_client_name(seq, nameHandle)
    }

    //#region Subscription

    fun subscribePort(subs: AlsaPortSubscription) {
        val err = Alsa.snd_seq_subscribe_port(seq, subs.handle)
        if (err != 0)
            throw AlsaException(err)
    }

    fun unsubscribePort(sub: AlsaPortSubscription) {
        Alsa.snd_seq_unsubscribe_port(seq, sub.handle)
    }

    fun queryPortSubscribers(query: AlsaSubscriptionQuery): Boolean {
        val ret = Alsa.snd_seq_query_port_subscribers(seq, query.handle)
        return ret == 0
    }

    // simplified SubscribePort()
    // formerly connectFrom()
    fun connectSource(portToReceive: Int, sourceClient: Int, sourcePort: Int) {
        val err = Alsa.snd_seq_connect_from(seq, portToReceive, sourceClient, sourcePort)
        if (err != 0)
            throw  AlsaException(err)
    }

    // simplified SubscribePort()
    // formerly connectTo()
    fun connectDestination(portToSendFrom: Int, destinationClient: Int, destinationPort: Int) {
        val err = Alsa.snd_seq_connect_to(seq, portToSendFrom, destinationClient, destinationPort)
        if (err != 0)
            throw AlsaException (err)
    }

    // simplified UnsubscribePort()
    // formerly disconnectFrom()
    fun disconnectSource(portToReceive: Int, sourceClient: Int, sourcePort: Int) {
        val err = Alsa.snd_seq_disconnect_from(seq, portToReceive, sourceClient, sourcePort)
        if (err != 0)
            throw  AlsaException(err)
    }

    // simplified UnsubscribePort()
    // formerly disconnectTo
    fun disconnectDestination(portToSendFrom: Int, destinationClient: Int, destinationPort: Int) {
        val err = Alsa.snd_seq_disconnect_to(seq, portToSendFrom, destinationClient, destinationPort)
        if (err != 0)
            throw  AlsaException(err)
    }

    //#endregion // Subscription

    fun resetPoolInput() {
        Alsa.snd_seq_reset_pool_input(seq)
    }

    fun resetPoolOutput() {
        Alsa.snd_seq_reset_pool_output(seq)
    }

    //#region Events

    private  val midiEventBufferSize : Long = 256
    private var eventBufferOutput = BytePointer(midiEventBufferSize)
    private var midiEventParserOutput: snd_midi_event_t? = null

    // FIXME: should this be moved to AlsaMidiApi? It's a bit too high level.
    fun send(port: Int, data: ByteArray, index: Int, count: Int) {
        if (midiEventParserOutput == null) {
            val ptr = snd_midi_event_t()
            Alsa.snd_midi_event_new(midiEventBufferSize, ptr)
            midiEventParserOutput = ptr
        }

        val ev = snd_seq_event_t(eventBufferOutput)
        for (i in index until index + count) {
            val ret = Alsa.snd_midi_event_encode_byte(midiEventParserOutput, data[i].toInt(), ev)
            if (ret < 0)
                throw  AlsaException(ret)
            if (ret == 1) {
                eventBufferOutput.put(seq_evt_off_source_port, port.toByte())
                eventBufferOutput.put(seq_evt_off_dest_client, AddressSubscribers.toByte())
                eventBufferOutput.put(seq_evt_off_dest_port, AddressUnknown.toByte())
                eventBufferOutput.put(seq_evt_off_queue, QueueDirect.toByte())
                Alsa.snd_seq_event_output_direct(seq, ev)
            }
        }
    }

    // receives messages as in ALSA sequencer format. Required for system annoucement messages.
    fun input( result:AlsaSequencerEvent, port: Int): Int {
        val evt = snd_seq_event_t()
        val ret = Alsa.snd_seq_event_input(seq, evt)
        if (ret >= 0) {
            result.type = evt.type()
            result.flags = evt.flags()
            result.tag = evt.tag()
            result.queue = evt.queue()
            result.time = evt.time()
            result.source = evt.source()
            result.dest = evt.dest()
        }
        return ret
    }

    private var midiEventParserInput: snd_midi_event_t? = null

    private fun prepareEventParser() {
        if (midiEventParserInput == null) {
            val ptr = snd_midi_event_t()
            Alsa.snd_midi_event_new(midiEventBufferSize, ptr)
            midiEventParserInput = ptr
        }
    }

    fun receive(port: Int, data: ByteArray, index: Int, count: Int): Int {
        var received = 0

        prepareEventParser()

        var remaining = true
        while (remaining && index + received < count) {
            val sevt = snd_seq_event_t()
            val ret = Alsa.snd_seq_event_input(seq, sevt)
            remaining = Alsa.snd_seq_event_input_pending(seq, 0) > 0
            if (ret < 0)
                throw AlsaException(ret)
            val converted = Alsa.snd_midi_event_decode(
                midiEventParserInput,
                ByteBuffer.wrap(data, index + received, data.size - index - received),
                (count - received).toLong(),
                sevt
            )
            if (converted < 0)
                throw AlsaException(converted.toInt())
            received += converted.toInt()
        }
        return received
    }

    private var eventLoopStopped = false
    private lateinit var eventLoopBuffer : ByteArray
    private val defaultInputTimeout = -1
    private var inputTimeout: Int = 0
    private var eventLoopTask: Job? = null
    private lateinit var onReceived: (ByteArray, Int, Int) -> Unit

    fun startListening(
        applicationPort: Int,
        buffer: ByteArray,
        onReceived: (ByteArray, Int, Int) -> Unit,
        timeout: Int = defaultInputTimeout
    ) {
        eventLoopBuffer = buffer
        inputTimeout = timeout
        this.onReceived = onReceived
        eventLoopTask = GlobalScope.launch { eventLoop (applicationPort) }
    }

    fun stopListening() {
        eventLoopStopped = true
    }

    private fun eventLoop(port: Int) {

        val pollfdSizeDummy = 8
        val count = Alsa.snd_seq_poll_descriptors_count(seq, POLLIN.toShort())
        val pollfdArrayRef = BytePointer((count * pollfdSizeDummy).toLong())
        val fd = pollfd()
        fd.put<BytePointer>(pollfdArrayRef)
        val ret = Alsa.snd_seq_poll_descriptors(seq, fd, count, POLLIN.toShort())
        if (ret < 0)
            throw AlsaException (ret)
        while (!eventLoopStopped) {
            val rt = HackyPoll.poll(fd, count.toLong(), inputTimeout)
            if (rt > 0) {
                val len = receive(port, eventLoopBuffer, 0, eventLoopBuffer.size)
                onReceived(eventLoopBuffer, 0, len)
            }
        }
    }

    //#endregion

    companion object {
        const val ClientSystem = 0
        const val POLLIN = 1

        private val seq_evt_size: Int
        private val seq_evt_off_source_port: Long
        private val seq_evt_off_dest_client: Long
        private val seq_evt_off_dest_port: Long
        private val seq_evt_off_queue: Long

        const val AddressUnknown = 253
        const val AddressSubscribers = 254
        const val AddressBroadcast = 255

        const val QueueDirect = 253

        init {
            Loader.load(snd_seq_t::class.java) // FIXME: this should not be required...
            seq_evt_size = sizeof(snd_seq_event_t::class.java)
            seq_evt_off_source_port =
                snd_seq_event_t.offsetof(snd_seq_event_t::class.java, "source") +
                        snd_seq_event_t.offsetof(snd_seq_addr_t::class.java, "port").toLong()
            seq_evt_off_dest_client =
                snd_seq_event_t.offsetof(snd_seq_event_t::class.java, "dest") +
                        snd_seq_event_t.offsetof(snd_seq_addr_t::class.java, "client").toLong()
            seq_evt_off_dest_port =
                snd_seq_event_t.offsetof(snd_seq_event_t::class.java, "dest") +
                        snd_seq_event_t.offsetof(snd_seq_addr_t::class.java, "port").toLong()
            seq_evt_off_queue = snd_seq_event_t.offsetof(snd_seq_event_t::class.java, "queue").toLong()
        }
    }

    init {
        val ptr = snd_seq_t()
        val err = Alsa.snd_seq_open(ptr, driverName, ioType, ioMode)
        if (err != 0)
            throw AlsaException(err)
        seq = ptr
    }
}


// This is a class for temporary managed class to make it possible to unmarshal via PtrToStructure.
//[StructLayout (LayoutKind.Sequential)]
class AlsaSequencerEvent {
    var type: Byte = 0
    var flags: Byte = 0
    var tag: Byte = 0
    var queue: Byte = 0
    var time: snd_seq_timestamp_t? = null
    var source: snd_seq_addr_t? = null
    var dest: snd_seq_addr_t? = null
    // FIXME: some of the struct members are arrays with SizeConsts, but the runtime (either mono or CoreCLR) does not accept them.
    // Therefore it is commented out, but that will result in inconsistent sizing between managed and unmanaged.
    //anonymous_type_3 data;

    val eventType: Int
        get() = type.toInt()
}

