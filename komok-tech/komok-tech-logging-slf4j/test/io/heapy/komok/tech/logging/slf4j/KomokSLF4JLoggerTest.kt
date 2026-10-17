package io.heapy.komok.tech.logging.slf4j

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LogHandler
import io.heapy.komok.tech.logging.LoggingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.time.Instant

class KomokSLF4JLoggerTest {
    @Test
    fun `SLF4J logger bridges to komok handler`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("com.example.Test") { ctx }

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
        val logger = KomokSLF4JLogger("test") { ctx }

        logger.info("hello {} and {}", "world", 42)

        assertEquals(1, events.size)
        assertEquals("hello world and 42", events[0].message)
    }

    @Test
    fun `SLF4J logger maps levels correctly`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("test") { ctx }

        logger.trace("t")
        logger.debug("d")
        logger.info("i")
        logger.warn("w")
        logger.error("e")

        assertEquals(
            listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR),
            events.map { it.level },
        )
    }

    @Test
    fun `SLF4J logger captures throwable`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val logger = KomokSLF4JLogger("test") { ctx }

        val exception = RuntimeException("boom")
        logger.error("error occurred", exception)

        assertEquals(1, events.size)
        val throwable = events[0].throwable
        assertNotNull(throwable)
        assertEquals("java.lang.RuntimeException", throwable.className)
    }

    @Test
    fun `SLF4J logger stamps events with the context clock`() {
        val events = mutableListOf<LogEvent>()
        val fixed = Instant.fromEpochMilliseconds(1_700_000_000_000)
        val ctx = LoggingContext(handler = { events.add(it) }, now = { fixed })
        val logger = KomokSLF4JLogger("test") { ctx }

        logger.info("stamped")

        assertEquals(fixed, events.single().timestamp)
    }

    @Test
    fun `SLF4J logger reports the pipeline level and skips rejected calls`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = MinLevelHandler(Level.WARN, events))
        val logger = KomokSLF4JLogger("test") { ctx }

        assertFalse(logger.isInfoEnabled)
        assertTrue(logger.isErrorEnabled)

        logger.info("dropped")
        logger.error("kept")

        assertEquals(listOf("kept"), events.map { it.message })
    }

    private class MinLevelHandler(
        private val minLevel: Level,
        private val events: MutableList<LogEvent>,
    ) : LogHandler {
        override fun handle(event: LogEvent) {
            events.add(event)
        }

        override fun isEnabled(
            level: Level,
            loggerName: String,
        ): Boolean =
            level.ordinal >= minLevel.ordinal
    }

    @Test
    fun `SLF4J logger without context is disabled and silent`() {
        val logger = KomokSLF4JLogger("test") { null }

        logger.info("should not throw")

        assertFalse(logger.isInfoEnabled)
    }

    @Test
    fun `SLF4J logger factory caches loggers`() {
        val factory = KomokLoggerFactory { null }

        val logger1 = factory.getLogger("test")
        val logger2 = factory.getLogger("test")

        assertEquals(logger1, logger2)
    }

    @Test
    fun `service provider reads the installed context on every call`() {
        val events = mutableListOf<LogEvent>()
        val provider = KomokSLF4JServiceProvider()
        provider.initialize()
        val logger = provider.loggerFactory.getLogger("test")
        assertFalse(logger.isInfoEnabled)

        KomokSLF4JServiceProvider.loggingContext = LoggingContext(handler = { events.add(it) })
        try {
            assertTrue(logger.isInfoEnabled)
            logger.info("after install")
        } finally {
            KomokSLF4JServiceProvider.loggingContext = null
        }

        assertEquals(1, events.size)
        assertEquals("after install", events[0].message)
    }
}
