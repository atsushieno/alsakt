package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test
import java.io.File

class AlsaSequencerTest {
    @Test
    fun simpleUse() {
        if (!File("/dev/snd/seq").exists()) {
            return
        }

        val seq = AlsaSequencer(AlsaIOType.Duplex, AlsaIOMode.NonBlocking)
        seq.close()
    }
}