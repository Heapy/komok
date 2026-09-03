package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class LogEventTest {
    @Test
    fun `event captures all fields`() {
        val timestamp = Clock.System.now()
        val mdc = Mdc.of(TestKeys.RequestId, "req-1")

        val event = LogEvent(
            level = Level.INFO,
            loggerName = "com.example.Test",
            message = "hello",
            throwable = null,
            timestamp = timestamp,
            mdc = mdc,
        )

        assertEquals(Level.INFO, event.level)
        assertEquals("com.example.Test", event.loggerName)
        assertEquals("hello", event.message)
        assertNull(event.throwable)
        assertEquals(timestamp, event.timestamp)
        assertEquals("req-1", event.mdc[TestKeys.RequestId])
    }

    @Test
    fun `threadName is lazy`() {
        val event = LogEvent(
            level = Level.INFO,
            loggerName = "test",
            message = "msg",
            throwable = null,
            timestamp = Clock.System.now(),
            mdc = Mdc.Empty,
        )

        val threadName = event.threadName
        assertNotNull(threadName)
        assertEquals(Thread.currentThread().name, threadName)
    }

    @Test
    fun `prepareForDeferredProcessing forces lazy fields`() {
        val event = LogEvent(
            level = Level.INFO,
            loggerName = "test",
            message = "msg",
            throwable = null,
            timestamp = Clock.System.now(),
            mdc = Mdc.Empty,
        )

        event.prepareForDeferredProcessing()
        assertNotNull(event.threadName)
    }

    @Test
    fun `event with throwable proxy`() {
        val proxy = ThrowableProxy.create(RuntimeException("boom"))
        val event = LogEvent(
            level = Level.ERROR,
            loggerName = "test",
            message = "error",
            throwable = proxy,
            timestamp = Clock.System.now(),
            mdc = Mdc.Empty,
        )

        assertNotNull(event.throwable)
        assertEquals("java.lang.RuntimeException", event.throwable!!.className)
    }

    private object TestKeys {
        object RequestId : Mdc.Key<String>("requestId")
    }
}
