package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AsyncHandlerTest {

    private fun event(message: String = "test", level: Level = Level.INFO) = LogEvent(
        level = level,
        loggerName = "test",
        message = message,
        throwable = null,
        timestamp = Clock.System.now(),
        mdc = Mdc.Empty,
    )

    @Test
    fun `async handler processes events`() {
        val latch = CountDownLatch(3)
        val events = CopyOnWriteArrayList<LogEvent>()

        val async = AsyncHandler(bufferSize = 64)
        val handler = async.wrap(LogHandler {
            events.add(it)
            latch.countDown()
        })

        handler.handle(event("msg1"))
        handler.handle(event("msg2"))
        handler.handle(event("msg3"))

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        async.close()

        assertEquals(3, events.size)
        assertEquals(3, async.enqueued.get())
        assertEquals(3, async.processed.get())
        assertEquals(0, async.dropped.get())
    }

    @Test
    fun `async handler DROP_ALL drops when buffer full`() {
        val events = CopyOnWriteArrayList<LogEvent>()
        val proceed = CountDownLatch(1)

        val async = AsyncHandler(
            bufferSize = 2,
            overflowPolicy = OverflowPolicy.DROP_ALL,
        )
        val handler = async.wrap(LogHandler {
            proceed.await()
            events.add(it)
        })

        // Fill buffer and overflow
        for (i in 1..10) {
            handler.handle(event("msg$i"))
        }

        // Some should be dropped
        assertTrue(async.dropped.get() > 0)
        assertEquals(async.enqueued.get() + async.dropped.get(), 10)

        proceed.countDown()
        async.close()
    }

    @Test
    fun `async handler HYBRID blocks for WARN and ERROR`() {
        val events = CopyOnWriteArrayList<LogEvent>()

        val async = AsyncHandler(
            bufferSize = 64,
            overflowPolicy = OverflowPolicy.HYBRID,
        )
        val handler = async.wrap(LogHandler { events.add(it) })

        handler.handle(event("info", Level.INFO))
        handler.handle(event("warn", Level.WARN))
        handler.handle(event("error", Level.ERROR))

        async.close()

        assertEquals(3, events.size)
    }

    @Test
    fun `async handler tracks metrics`() {
        val async = AsyncHandler(bufferSize = 64)
        val handler = async.wrap(LogHandler { })

        handler.handle(event())
        handler.handle(event())

        async.close()

        assertEquals(2, async.enqueued.get())
        assertEquals(2, async.processed.get())
        assertEquals(0, async.dropped.get())
        assertEquals(0, async.failed.get())
    }

    @Test
    fun `async handler counts failures`() {
        val latch = CountDownLatch(2)

        val async = AsyncHandler(bufferSize = 64)
        val handler = async.wrap(LogHandler {
            latch.countDown()
            throw RuntimeException("handler error")
        })

        handler.handle(event())
        handler.handle(event())

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        async.close()

        assertEquals(2, async.failed.get())
        assertEquals(0, async.processed.get())
    }

    @Test
    fun `prepareForDeferredProcessing is called`() {
        val events = CopyOnWriteArrayList<LogEvent>()
        val latch = CountDownLatch(1)

        val async = AsyncHandler(bufferSize = 64)
        val handler = async.wrap(LogHandler {
            events.add(it)
            latch.countDown()
        })

        handler.handle(event())

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        async.close()

        // threadName should have been forced by prepareForDeferredProcessing
        assertTrue(events[0].threadName.isNotEmpty())
    }
}
