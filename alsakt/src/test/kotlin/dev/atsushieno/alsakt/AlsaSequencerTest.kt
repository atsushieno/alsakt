package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test

class AlsaSequencerTest {
    @Test
    fun simpleUse() {
        val seq = AlsaSequencer(AlsaIOType.Duplex, AlsaIOMode.NonBlocking)
        seq.close()
    }
}