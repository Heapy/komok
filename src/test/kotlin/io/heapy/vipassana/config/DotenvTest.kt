package io.heapy.vipassana.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DotenvTest {
    @Test
    fun `ignore if missing`() {
        assertNull(Dotenv.new()["NO_SUCH_ENV"])
    }

    @Test
    fun `default if missing`() {
        assertEquals(
            "default",
            Dotenv.new().getOrDefault("NO_SUCH_ENV", "default")
        )
    }

    @Test
    fun `should throw exception`() {
        assertThrows<IllegalStateException> {
            Dotenv.new().require("NO_SUCH_ENV")
        }
    }

    @Test
    fun `parsing with empty line`() {
        val dotenv = Dotenv.getEnvironmentVariables(
            listOf(
                "TEST_VAR=Hello, Vipassana!",
                ""
            ),
            mapOf()
        )

        assertEquals("Hello, Vipassana!", dotenv["TEST_VAR"])
    }

    @Test
    fun `parsing with empty value`() {
        val dotenv = Dotenv.getEnvironmentVariables(
            listOf(
                "TEST_VAR=",
                ""
            ),
            mapOf()
        )

        assertEquals("", dotenv["TEST_VAR"])
    }

    @Test
    fun `parsing with multiple equals`() {
        val dotenv = Dotenv.getEnvironmentVariables(
            listOf(
                "TEST_VAR===",
                ""
            ),
            mapOf()
        )

        assertEquals("==", dotenv["TEST_VAR"])
    }

    @Test
    fun `parsing with invalid format`() {
        val illegalStateException = assertThrows<IllegalStateException> {
            Dotenv.getEnvironmentVariables(
                listOf(
                    "TEST_VAR",
                    ""
                ),
                mapOf()
            )
        }

        assertEquals("""Invalid config line 0: "TEST_VAR"""", illegalStateException.message)
    }
}
