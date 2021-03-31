package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test

class AlsaClientInfoTest {
    @Test
    fun instancing() {
        val info = AlsaClientInfo()
        info.close()
    }
}