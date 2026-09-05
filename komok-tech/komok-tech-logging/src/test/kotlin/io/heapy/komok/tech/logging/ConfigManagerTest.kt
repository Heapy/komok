package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigManagerTest {
    @Test
    fun `current returns initial context`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val manager = ConfigManager(ctx)

        val log = logger<ConfigManagerTest>()
        with(manager.current) {
            log.info { "test" }
        }

        assertEquals(1, events.size)
    }

    @Test
    fun `reload swaps context atomically`() {
        val events1 = mutableListOf<LogEvent>()
        val events2 = mutableListOf<LogEvent>()

        val ctx1 = LoggingContext(handler = { events1.add(it) })
        val ctx2 = LoggingContext(handler = { events2.add(it) })

        val manager = ConfigManager(ctx1)
        val log = logger<ConfigManagerTest>()

        with(manager.current) {
            log.info { "before reload" }
        }

        manager.reload(ctx2)

        with(manager.current) {
            log.info { "after reload" }
        }

        assertEquals(1, events1.size)
        assertEquals(1, events2.size)
        assertEquals("before reload", events1[0].message)
        assertEquals("after reload", events2[0].message)
    }
}
