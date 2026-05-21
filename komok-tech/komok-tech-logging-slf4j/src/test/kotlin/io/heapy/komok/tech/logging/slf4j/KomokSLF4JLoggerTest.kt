package io.heapy.komok.tech.logging.slf4j

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LogHandler
import io.heapy.komok.tech.logging.LoggingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class KomokSLF4JLoggerTest {
    @Test
    fun `SLF4J logger bridges to komok handler`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("com.example.Test", ctx)

        logger.info("hello from SLF4J")

        assertEquals(1, events.size)
        assertEquals(Level.INFO, events[0].level)
        assertEquals("hello from SLF4J", events[0].message)
        assertEquals("com.example.Test", events[0].loggerName)
    }

    @Test
    fun `SLF4J logger formats parameterized messages`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("test", ctx)

        logger.info("hello {} and {}", "world", 42)

        assertEquals(1, events.size)
        assertEquals("hello world and 42", events[0].message)
    }

    @Test
    fun `SLF4J logger maps levels correctly`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("test", ctx)

        logger.trace("t")
        logger.debug("d")
        logger.info("i")
        logger.warn("w")
        logger.error("e")

        assertEquals(5, events.size)
        assertEquals(Level.TRACE, events[0].level)
        assertEquals(Level.DEBUG, events[1].level)
        assertEquals(Level.INFO, events[2].level)
        assertEquals(Level.WARN, events[3].level)
        assertEquals(Level.ERROR, events[4].level)
    }

    @Test
    fun `SLF4J logger captures throwable`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("test", ctx)

        val exception = RuntimeException("boom")
        logger.error("error occurred", exception)

        assertEquals(1, events.size)
        assertNotNull(events[0].throwable)
        assertEquals("java.lang.RuntimeException", events[0].throwable!!.className)
    }

    @Test
    fun `SLF4J logger with null context is silent`() {
        val logger = KomokSLF4JLogger("test", null)
        logger.info("should not throw")
    }

    @Test
    fun `SLF4J logger factory caches loggers`() {
        val ctx = LoggingContext(handler = LogHandler.noop())
        val factory = KomokLoggerFactory(ctx)

        val logger1 = factory.getLogger("test")
        val logger2 = factory.getLogger("test")

        assertEquals(logger1, logger2)
    }
}
