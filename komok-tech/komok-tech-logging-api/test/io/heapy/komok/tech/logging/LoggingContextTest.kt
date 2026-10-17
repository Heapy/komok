package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LoggingContextTest {
    private object RequestId : Mdc.Key<String>("requestId")
    private object UserId : Mdc.Key<Long>("userId")

    @Test
    fun `withMdc derives context with additional entries`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })

        val derived = ctx.withMdc(RequestId, "req-1")
        val log = logger<LoggingContextTest>()

        with(derived) {
            log.info { "test" }
        }

        assertEquals("req-1", events[0].mdc[RequestId])
    }

    @Test
    fun `withMdc does not modify original context`() {
        val ctx = LoggingContext(handler = LogHandler.noop())

        val _ = ctx.withMdc(RequestId, "req-1")

        assertNull(ctx.mdc[RequestId])
    }

    @Test
    fun `withMdc merges MDC entries`() {
        val ctx = LoggingContext(
            handler = LogHandler.noop(),
            mdc = Mdc.of(RequestId, "req-1"),
        )

        val derived = ctx.withMdc(Mdc.of(UserId, 42L))

        assertEquals("req-1", derived.mdc[RequestId])
        assertEquals(42L, derived.mdc[UserId])
    }

    @Test
    fun `withMdc builder DSL works`() {
        val ctx = LoggingContext(handler = LogHandler.noop())

        val derived = ctx.withMdc {
            RequestId to "req-1"
            UserId to 42L
        }

        assertEquals("req-1", derived.mdc[RequestId])
        assertEquals(42L, derived.mdc[UserId])
    }

    @Test
    fun `withMdc free function derives and runs block`() {
        val events = mutableListOf<LogEvent>()
        val ctx = LoggingContext(handler = { events.add(it) })
        val log = logger<LoggingContextTest>()

        with(ctx) {
            withMdc(RequestId, "req-1") {
                log.info { "inside derived" }
            }
        }

        assertEquals("req-1", events[0].mdc[RequestId])
    }
}
