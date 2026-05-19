package io.heapy.komok.tech.logging.jul

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LoggingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.logging.LogRecord

class KomokJulHandlerTest {
    @Test
    fun `JUL handler bridges to komok handler`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val handler = KomokJulHandler(ctx)

        val record = LogRecord(java.util.logging.Level.INFO, "hello from JUL")
        record.loggerName = "com.example.JulTest"
        handler.publish(record)

        assertEquals(1, events.size)
        assertEquals(Level.INFO, events[0].level)
        assertEquals("hello from JUL", events[0].message)
        assertEquals("com.example.JulTest", events[0].loggerName)
    }

    @Test
    fun `JUL handler maps levels correctly`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val handler = KomokJulHandler(ctx)

        data class LevelMapping(
            val julLevel: java.util.logging.Level,
            val level: Level,
        )

        val levels = listOf(
            LevelMapping(java.util.logging.Level.ALL, Level.TRACE),
            LevelMapping(java.util.logging.Level.FINEST, Level.TRACE),
            LevelMapping(java.util.logging.Level.FINER, Level.TRACE),
            LevelMapping(java.util.logging.Level.FINE, Level.DEBUG),
            LevelMapping(java.util.logging.Level.INFO, Level.INFO),
            LevelMapping(java.util.logging.Level.WARNING, Level.WARN),
            LevelMapping(java.util.logging.Level.SEVERE, Level.ERROR),
        )

        for ((julLevel) in levels) {
            val record = LogRecord(julLevel, "msg")
            record.loggerName = "test"
            handler.publish(record)
        }

        assertEquals(7, events.size)
        assertEquals(Level.TRACE, events[0].level)
        assertEquals(Level.TRACE, events[1].level)
        assertEquals(Level.TRACE, events[2].level)
        assertEquals(Level.DEBUG, events[3].level)
        assertEquals(Level.INFO, events[4].level)
        assertEquals(Level.WARN, events[5].level)
        assertEquals(Level.ERROR, events[6].level)
    }

    @Test
    fun `JUL handler captures throwable`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val handler = KomokJulHandler(ctx)

        val record = LogRecord(java.util.logging.Level.SEVERE, "error occurred")
        record.loggerName = "test"
        record.thrown = RuntimeException("boom")
        handler.publish(record)

        assertEquals(1, events.size)
        assertNotNull(events[0].throwable)
        assertEquals("java.lang.RuntimeException", events[0].throwable!!.className)
    }

    @Test
    fun `JUL handler ignores null records`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val handler = KomokJulHandler(ctx)

        handler.publish(null)
        assertEquals(0, events.size)
    }
}
