package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class FormatEncoderTest {

    private object RequestId : Mdc.Key<String>("requestId")

    private val timestamp = Instant.parse("2025-01-15T10:30:00Z")

    private fun event(
        message: String = "test message",
        level: Level = Level.INFO,
        loggerName: String = "com.example.MyService",
        mdc: Mdc = Mdc.Empty,
        throwable: ThrowableProxy? = null,
    ) = LogEvent(
        level = level,
        loggerName = loggerName,
        message = message,
        throwable = throwable,
        timestamp = timestamp,
        mdc = mdc,
    )

    @Test
    fun `format with timestamp and message`() {
        val encoder = format {
            timestamp()
            literal(" ")
            message()
        }

        assertEquals("2025-01-15T10:30:00Z test message", encoder.encode(event()))
    }

    @Test
    fun `format with level padded to 5 chars`() {
        val encoder = format { level() }

        assertEquals("INFO ", encoder.encode(event(level = Level.INFO)))
        assertEquals("WARN ", encoder.encode(event(level = Level.WARN)))
        assertEquals("ERROR", encoder.encode(event(level = Level.ERROR)))
    }

    @Test
    fun `format with logger name`() {
        val encoder = format { loggerName() }
        assertEquals("com.example.MyService", encoder.encode(event()))
    }

    @Test
    fun `format with short logger name`() {
        val encoder = format { loggerShort() }
        assertEquals("MyService", encoder.encode(event()))
    }

    @Test
    fun `format with short logger name multiple segments`() {
        val encoder = format { loggerShort(2) }
        assertEquals("example.MyService", encoder.encode(event()))
    }

    @Test
    fun `format with MDC value`() {
        val encoder = format { mdc("requestId") }
        val mdc = Mdc.of(RequestId, "req-123")

        assertEquals("req-123", encoder.encode(event(mdc = mdc)))
    }

    @Test
    fun `format with missing MDC value returns empty`() {
        val encoder = format { mdc("requestId") }
        assertEquals("", encoder.encode(event()))
    }

    @Test
    fun `format with all MDC`() {
        val encoder = format { allMdc() }
        val mdc = Mdc.of(RequestId, "req-123")

        assertEquals("requestId=req-123", encoder.encode(event(mdc = mdc)))
    }

    @Test
    fun `format with throwable trace`() {
        val proxy = ThrowableProxy.create(RuntimeException("boom"))
        val encoder = format {
            message()
            throwableTrace()
        }

        val result = encoder.encode(event(throwable = proxy))
        assertTrue(result.contains("test message"))
        assertTrue(result.contains("java.lang.RuntimeException: boom"))
    }

    @Test
    fun `full format pattern`() {
        val encoder = format {
            timestamp()
            literal(" [")
            level()
            literal("] [")
            threadName()
            literal("] ")
            loggerShort()
            literal(" - ")
            message()
        }

        val result = encoder.encode(event())
        assertTrue(result.startsWith("2025-01-15T10:30:00Z [INFO "))
        assertTrue(result.contains("MyService - test message"))
    }

    @Test
    fun `newline element`() {
        val encoder = format {
            message()
            newline()
            level()
        }

        val result = encoder.encode(event())
        assertTrue(result.contains("\n"))
    }
}
