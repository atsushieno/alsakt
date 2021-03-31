package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test

class AlsaSubscriptionQueryTest {
    @Test
    fun instancing() {
        val info = AlsaSubscriptionQuery()
        info.close()
    }
}