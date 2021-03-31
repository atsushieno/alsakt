package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test

class AlsaPortInfoTest {
    @Test
    fun instancing() {
        val info = AlsaPortInfo()
        info.close()
    }
}