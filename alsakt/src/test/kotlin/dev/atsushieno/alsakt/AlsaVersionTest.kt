package dev.atsushieno.alsakt

import org.junit.jupiter.api.Test

class AlsaVersionTest {
    @Test
    fun getVersion() {
        val ver = AlsaVersion.versionString
        assert(ver.length > 0)
        assert(AlsaVersion.major > 0) // it would not be version 0...
        assert(AlsaVersion.minor >= 0) // it would be non-negative
        assert(AlsaVersion.revision >= 0) // it would be non-negative
    }
}