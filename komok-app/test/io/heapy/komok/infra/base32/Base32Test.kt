package io.heapy.komok.infra.base32

import io.heapy.kotomi.runtime.*
import io.heapy.komok.UnitTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*

class Base32Test {
    @UnitTest
    fun `decode 16 bytes`() = runTest {
        val base32 = graph<Base32Module>().let { it { base32() } }

        val decoded = base32.decode("A234A234A234A234")

        assertArrayEquals(
            byteArrayOf(6, -73, -64, 107, 124, 6, -73, -64, 107, 124),
            decoded,
        )
    }

    @UnitTest
    fun `decode invalid bytes`() = runTest {
        val base32 = graph<Base32Module>().let { it { base32() } }

        val exception = assertThrows(IllegalArgumentException::class.java) {
            val _ = base32.decode("1234A234A234A234")
        }

        assertEquals(
            "Invalid character in Base32 string: 1234A234A234A234",
            exception.message,
        )
    }
}
