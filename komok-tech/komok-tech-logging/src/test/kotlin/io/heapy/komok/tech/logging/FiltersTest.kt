package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class FiltersTest {

    private fun event(
        level: Level = Level.INFO,
        loggerName: String = "com.example.Test",
    ) = LogEvent(
        level = level,
        loggerName = loggerName,
        message = "test",
        throwable = null,
        timestamp = Clock.System.now(),
        mdc = Mdc.Empty,
    )

    @Test
    fun `LevelFilter passes events at or above threshold`() {
        val events = mutableListOf<LogEvent>()
        val handler = LevelFilter(Level.WARN).wrap(LogHandler { events.add(it) })

        handler.handle(event(level = Level.TRACE))
        handler.handle(event(level = Level.DEBUG))
        handler.handle(event(level = Level.INFO))
        handler.handle(event(level = Level.WARN))
        handler.handle(event(level = Level.ERROR))

        assertEquals(2, events.size)
        assertEquals(Level.WARN, events[0].level)
        assertEquals(Level.ERROR, events[1].level)
    }

    @Test
    fun `LevelFilter with TRACE passes everything`() {
        val events = mutableListOf<LogEvent>()
        val handler = LevelFilter(Level.TRACE).wrap(LogHandler { events.add(it) })

        handler.handle(event(level = Level.TRACE))
        handler.handle(event(level = Level.ERROR))

        assertEquals(2, events.size)
    }

    @Test
    fun `LevelFilter with OFF blocks everything`() {
        val events = mutableListOf<LogEvent>()
        val handler = LevelFilter(Level.OFF).wrap(LogHandler { events.add(it) })

        handler.handle(event(level = Level.ERROR))

        assertEquals(0, events.size)
    }

    @Test
    fun `LoggerNameFilter matches prefix`() {
        val events = mutableListOf<LogEvent>()
        val handler = LoggerNameFilter("com.example").wrap(LogHandler { events.add(it) })

        handler.handle(event(loggerName = "com.example.Test"))
        handler.handle(event(loggerName = "com.example"))
        handler.handle(event(loggerName = "com.other.Test"))
        handler.handle(event(loggerName = "com.exampleFoo"))

        assertEquals(2, events.size)
    }

    @Test
    fun `LoggerNameFilter exact match`() {
        val events = mutableListOf<LogEvent>()
        val handler = LoggerNameFilter("com.example.Test", matchExact = true)
            .wrap(LogHandler { events.add(it) })

        handler.handle(event(loggerName = "com.example.Test"))
        handler.handle(event(loggerName = "com.example.Test.Sub"))

        assertEquals(1, events.size)
    }

    @Test
    fun `PredicateFilter with custom predicate`() {
        val events = mutableListOf<LogEvent>()
        val handler = PredicateFilter { it.level.ordinal >= Level.WARN.ordinal }
            .wrap(LogHandler { events.add(it) })

        handler.handle(event(level = Level.INFO))
        handler.handle(event(level = Level.WARN))

        assertEquals(1, events.size)
    }

    @Test
    fun `SamplingFilter passes every Nth event`() {
        val events = mutableListOf<LogEvent>()
        val handler = SamplingFilter(3).wrap(LogHandler { events.add(it) })

        for (i in 1..9) {
            handler.handle(event())
        }

        assertEquals(3, events.size)
    }
}
