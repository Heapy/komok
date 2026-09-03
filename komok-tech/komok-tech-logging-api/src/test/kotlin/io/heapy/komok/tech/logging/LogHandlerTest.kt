package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class LogHandlerTest {
    @Test
    fun `noop handler discards events`() {
        val handler = LogHandler.noop()
        handler.handle(testEvent("discarded"))
    }

    @Test
    fun `composite dispatches to all handlers`() {
        val events1 = mutableListOf<LogEvent>()
        val events2 = mutableListOf<LogEvent>()

        val handler = LogHandler.composite(
            { events1.add(it) },
            { events2.add(it) },
        )

        handler.handle(testEvent("hello"))

        assertEquals(1, events1.size)
        assertEquals(1, events2.size)
        assertEquals("hello", events1[0].message)
        assertEquals("hello", events2[0].message)
    }

    @Test
    fun `composite of empty list returns noop`() {
        val handler = LogHandler.composite(emptyList())
        handler.handle(testEvent("test"))
    }

    @Test
    fun `composite of single handler returns that handler`() {
        val events = mutableListOf<LogEvent>()
        val single = LogHandler { events.add(it) }
        val handler = LogHandler.composite(listOf(single))

        handler.handle(testEvent("test"))
        assertEquals(1, events.size)
    }

    @Test
    fun `wrappedBy applies wrappers outside-in`() {
        val events = mutableListOf<String>()

        val wrapper1 = LogHandlerWrapper { next ->
            LogHandler { event ->
                events.add("before-1")
                next.handle(event)
                events.add("after-1")
            }
        }
        val wrapper2 = LogHandlerWrapper { next ->
            LogHandler { event ->
                events.add("before-2")
                next.handle(event)
                events.add("after-2")
            }
        }

        val handler = LogHandler { events.add("handle") }
            .wrappedBy(wrapper1, wrapper2)

        handler.handle(testEvent("test"))

        assertEquals(
            listOf("before-1", "before-2", "handle", "after-2", "after-1"),
            events,
        )
    }

    @Test
    fun `andThen chains wrappers`() {
        val events = mutableListOf<String>()

        val outer = LogHandlerWrapper { next ->
            LogHandler { event ->
                events.add("outer")
                next.handle(event)
            }
        }
        val inner = LogHandlerWrapper { next ->
            LogHandler { event ->
                events.add("inner")
                next.handle(event)
            }
        }

        val combined = outer andThen inner
        val handler = combined.wrap { events.add("terminal") }
        handler.handle(testEvent("test"))

        assertEquals(listOf("outer", "inner", "terminal"), events)
    }

    @Test
    fun `identity wrapper passes through`() {
        val events = mutableListOf<LogEvent>()
        val identity = LogHandlerWrapper.identity()
        val handler = identity.wrap { events.add(it) }

        handler.handle(testEvent("test"))
        assertEquals(1, events.size)
    }

    private fun testEvent(message: String) = LogEvent(
        level = Level.INFO,
        loggerName = "test",
        message = message,
        throwable = null,
        timestamp = Clock.System.now(),
        mdc = Mdc.Empty,
    )
}
