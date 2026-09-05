package io.heapy.komok.tech.logging

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Overflow policy for [AsyncHandler] when the buffer is full.
 */
enum class OverflowPolicy {
    /** Drop any event when buffer full. */
    DROP_ALL,
    /** Block all events until space available. */
    BLOCK_ALL,
    /** Drop TRACE/DEBUG/INFO; block WARN/ERROR up to timeout. */
    HYBRID,
}

/**
 * Coroutine-based async handler wrapper.
 * Dispatches events through a [Channel] to a background coroutine.
 *
 * Call [close] on shutdown to drain remaining events.
 */
class AsyncHandler(
    private val bufferSize: Int = 1024,
    private val overflowPolicy: OverflowPolicy = OverflowPolicy.DROP_ALL,
    private val highPriorityBlockTimeout: Duration = 50.milliseconds,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LogHandlerWrapper, AutoCloseable {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private lateinit var channel: Channel<LogEvent>
    private lateinit var job: Job

    val enqueued = AtomicLong(0)
    val processed = AtomicLong(0)
    val dropped = AtomicLong(0)
    val failed = AtomicLong(0)

    override fun wrap(next: LogHandler): LogHandler {
        channel = Channel(bufferSize)
        job = scope.launch {
            for (event in channel) {
                try {
                    next.handle(event)
                    processed.incrementAndGet()
                } catch (_: Exception) {
                    failed.incrementAndGet()
                }
            }
        }
        return LogHandler { event ->
            event.prepareForDeferredProcessing()
            when (overflowPolicy) {
                OverflowPolicy.DROP_ALL -> {
                    val result = channel.trySend(event)
                    if (result.isSuccess) {
                        enqueued.incrementAndGet()
                    } else {
                        dropped.incrementAndGet()
                    }
                }
                OverflowPolicy.BLOCK_ALL -> {
                    runBlocking {
                        channel.send(event)
                    }
                    enqueued.incrementAndGet()
                }
                OverflowPolicy.HYBRID -> {
                    if (event.level.ordinal >= Level.WARN.ordinal) {
                        val sent = runBlocking {
                            withTimeoutOrNull(highPriorityBlockTimeout) {
                                channel.send(event)
                                true
                            }
                        }
                        if (sent == true) {
                            enqueued.incrementAndGet()
                        } else {
                            dropped.incrementAndGet()
                        }
                    } else {
                        val result = channel.trySend(event)
                        if (result.isSuccess) {
                            enqueued.incrementAndGet()
                        } else {
                            dropped.incrementAndGet()
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        channel.close()
        runBlocking { job.join() }
        scope.cancel()
    }

    override fun toString(): String = "AsyncHandler(buffer=$bufferSize, policy=$overflowPolicy)"
}
