package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DslTest {
    private object RequestId : Mdc.Key<String>("requestId")
    private object TenantId : Mdc.Key<String>("tenantId")

    @Test
    fun `loggingContext with console appender`() {
        val events = mutableListOf<String>()

        val ctx = loggingContext {
            handler { events.add(it.message) }
        }

        val log = logger<DslTest>()
        with(ctx) {
            log.info { "hello" }
        }

        assertEquals(listOf("hello"), events)
    }

    @Test
    fun `loggingContext with level filter`() {
        val events = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            level(Level.WARN)
            handler { events.add(it) }
        }

        val log = logger<DslTest>()
        with(ctx) {
            log.info { "filtered out" }
            log.warn { "passes" }
            log.error { "passes too" }
        }

        assertEquals(2, events.size)
        assertEquals(Level.WARN, events[0].level)
        assertEquals(Level.ERROR, events[1].level)
    }

    @Test
    fun `loggingContext with route`() {
        val audit = mutableListOf<LogEvent>()
        val general = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            route {
                logger("audit") {
                    handler { audit.add(it) }
                }
                otherwise {
                    handler { general.add(it) }
                }
            }
        }

        with(ctx) {
            logger("audit").info { "audit event" }
            logger("other").info { "general event" }
        }

        assertEquals(1, audit.size)
        assertEquals(1, general.size)
    }

    @Test
    fun `loggingContext with MDC route`() {
        val acme = mutableListOf<LogEvent>()
        val other = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            routeByMdc(TenantId) {
                value("acme") {
                    handler { acme.add(it) }
                }
                otherwise {
                    handler { other.add(it) }
                }
            }
        }

        val log = logger<DslTest>()
        with(ctx.withMdc(TenantId, "acme")) {
            log.info { "acme event" }
        }
        with(ctx.withMdc(TenantId, "globex")) {
            log.info { "globex event" }
        }

        assertEquals(1, acme.size)
        assertEquals(1, other.size)
    }

    @Test
    fun `loggingContext with pipeline nesting`() {
        val events = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            pipeline {
                level(Level.ERROR)
                handler { events.add(it) }
            }
        }

        val log = logger<DslTest>()
        with(ctx) {
            log.info { "filtered" }
            log.error { "passes" }
        }

        assertEquals(1, events.size)
    }

    @Test
    fun `loggingContext with tee`() {
        val errors = mutableListOf<LogEvent>()
        val all = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            tee {
                branch({ it.level.ordinal >= Level.ERROR.ordinal }) {
                    handler { errors.add(it) }
                }
                all {
                    handler { all.add(it) }
                }
            }
        }

        val log = logger<DslTest>()
        with(ctx) {
            log.info { "info" }
            log.error { "error" }
        }

        assertEquals(1, errors.size)
        assertEquals(2, all.size)
    }

    @Test
    fun `loggingContext with enrich`() {
        val events = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            enrich(RequestId, "enriched-req")
            handler { events.add(it) }
        }

        val log = logger<DslTest>()
        with(ctx) {
            log.info { "test" }
        }

        assertEquals("enriched-req", events[0].mdc[RequestId])
    }

    @Test
    fun `loggingContext with filter predicate`() {
        val events = mutableListOf<LogEvent>()

        val ctx = loggingContext {
            filter { it.message.contains("important") }
            handler { events.add(it) }
        }

        val log = logger<DslTest>()
        with(ctx) {
            log.info { "not relevant" }
            log.info { "this is important" }
        }

        assertEquals(1, events.size)
        assertTrue(events[0].message.contains("important"))
    }
}
