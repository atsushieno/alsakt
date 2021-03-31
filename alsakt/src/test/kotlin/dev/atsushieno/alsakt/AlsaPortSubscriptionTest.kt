package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test

class AlsaPortSubscriptionTest {
    @Test
    fun instancing() {
        val info = AlsaPortSubscription()
        info.close()
    }
}