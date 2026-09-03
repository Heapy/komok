package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoggerTest {
    suspend fun delay(time: Long) {
        suspendCoroutine { cont ->
            Thread.sleep(time)
            cont.resume(Unit)
        }
    }

    @Test
    suspend fun `logger captures events at all levels`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val log = logger<LoggerTest>()

        with(ctx) {
            log.trace { delay(100); "trace msg" }
            log.debug { "debug msg" }
            log.info { "info msg" }
            log.warn { "warn msg" }
            log.error { "error msg" }
        }

        assertEquals(5, events.size)
        assertEquals(Level.TRACE, events[0].level)
        assertEquals(Level.DEBUG, events[1].level)
        assertEquals(Level.INFO, events[2].level)
        assertEquals(Level.WARN, events[3].level)
        assertEquals(Level.ERROR, events[4].level)
    }

    @Test
    fun `logger name from reified type`() {
        val log = logger<LoggerTest>()
        assertEquals("io.heapy.komok.tech.logging.LoggerTest", log.name)
    }

    @Test
    fun `logger name from string`() {
        val log = logger("com.example.MyService")
        assertEquals("com.example.MyService", log.name)
    }

    @Test
    fun `logger captures throwable`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val log = logger<LoggerTest>()
        val exception = RuntimeException("boom")

        with(ctx) {
            log.error(exception) { "error with throwable" }
        }

        assertEquals(1, events.size)
        assertEquals("java.lang.RuntimeException", events[0].throwable?.className)
        assertEquals("boom", events[0].throwable?.message)
    }

    @Test
    fun `logger captures mdc from context`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(
            handler = { events.add(it) },
            mdc = Mdc.of(TestRequestId, "req-123"),
        )
        val log = logger<LoggerTest>()

        with(ctx) {
            log.info { "with mdc" }
        }

        assertEquals("req-123", events[0].mdc[TestRequestId])
    }

    @Test
    fun `message lambda is invoked`() {
        var invoked = false
        val ctx = LoggingContext(handler = { })
        val log = logger<LoggerTest>()

        with(ctx) {
            log.info {
                invoked = true
                "hello"
            }
        }

        assertEquals(true, invoked)
    }

    private object TestRequestId : Mdc.Key<String>("requestId")
}
