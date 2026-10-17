package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class JsonEncoderTest {

    private val timestamp = Instant.parse("2025-01-15T10:30:00Z")

    private fun event(
        message: String = "test",
        mdc: Mdc = Mdc.Empty,
    ) = LogEvent(
        level = Level.INFO,
        loggerName = "com.example.Test",
        message = message,
        throwable = null,
        timestamp = timestamp,
        mdc = mdc,
    )

    @Test
    fun `json output contains core fields`() {
        val encoder = JsonEncoder()
        val result = encoder.encode(event())

        assertTrue(result.contains("\"timestamp\":\"2025-01-15T10:30:00Z\""))
        assertTrue(result.contains("\"level\":\"INFO\""))
        assertTrue(result.contains("\"logger\":\"com.example.Test\""))
        assertTrue(result.contains("\"message\":\"test\""))
    }

    @Test
    fun `json output includes MDC`() {
        val encoder = JsonEncoder()
        val mdc = Mdc.of(object : Mdc.Key<String>("requestId") {}, "req-123")
        val result = encoder.encode(event(mdc = mdc))

        assertTrue(result.contains("\"requestId\":\"req-123\""))
    }

    @Test
    fun `json output includes static fields`() {
        val encoder = JsonEncoder(staticFields = mapOf("env" to "prod"))
        val result = encoder.encode(event())

        assertTrue(result.contains("\"env\":\"prod\""))
    }

    @Test
    fun `json escapes special characters`() {
        val encoder = JsonEncoder()
        val result = encoder.encode(event(message = "line1\nline2\ttab\"quote"))

        assertTrue(result.contains("\\n"))
        assertTrue(result.contains("\\t"))
        assertTrue(result.contains("\\\"quote"))
    }

    @Test
    fun `pretty print format`() {
        val encoder = JsonEncoder(prettyPrint = true)
        val result = encoder.encode(event())

        assertTrue(result.contains("{\n"))
        assertTrue(result.contains("\n}"))
    }
}
