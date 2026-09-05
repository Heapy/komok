package io.heapy.komok.tech.runtime.wrappers

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.logging.logger
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class WrappersTest {
    private val meta = MethodMeta(
        owner = "com.example.Svc",
        name = "doWork",
        parameters = emptyList(),
        returnType = "kotlin.String",
        isSuspend = true,
    )

    @Test
    fun `retryCall retries until success`() = runTest {
        val attempts = AtomicInteger(0)

        val result = retryCall(meta, maxAttempts = 3, delayMillis = 0) {
            if (attempts.incrementAndGet() < 3) error("transient") else "ok"
        }

        assertAll(
            { assertEquals("ok", result) },
            { assertEquals(3, attempts.get()) },
        )
    }

    @Test
    fun `retryCall gives up after maxAttempts`() = runTest {
        val attempts = AtomicInteger(0)

        val failure = runCatching {
            retryCall(meta, maxAttempts = 2, delayMillis = 0) {
                attempts.incrementAndGet()
                throw IllegalStateException("nope")
            }
        }

        assertAll(
            { assertTrue(failure.isFailure) },
            { assertInstanceOf<IllegalStateException>(failure.exceptionOrNull()) },
            { assertEquals(2, attempts.get()) },
        )
    }

    @Test
    fun `retryCall never retries cancellation`() = runTest {
        val attempts = AtomicInteger(0)

        val failure = runCatching {
            retryCall(meta, maxAttempts = 5, delayMillis = 0) {
                attempts.incrementAndGet()
                throw CancellationException("cancel")
            }
        }

        assertAll(
            { assertTrue(failure.isFailure) },
            { assertEquals(1, attempts.get()) },
        )
    }

    @Test
    fun `metricCall records a timed sample in the caller-provided registry`() = runTest {
        val registry = SimpleMeterRegistry()

        val result = withRegistry(registry) {
            metricCall(meta, name = "") { "done" }
        }

        val timer = registry.find("com.example.Svc.doWork").tag("outcome", "success").timer()
        assertAll(
            { assertEquals("done", result) },
            { assertNotNull(timer) },
            { assertEquals(1L, timer?.count()) },
        )
        registry.close()
    }

    @Test
    fun `logCall logs to the caller-provided logging context and returns the value`() = runTest {
        val messages = CopyOnWriteArrayList<String>()
        val loggingContext = LoggingContext(handler = { messages += it.message })

        val result = withLogging(loggingContext) {
            logCall(
                logger = logger("test"),
                meta = meta,
                level = Level.DEBUG,
                includeResult = true,
                includeDuration = true,
            ) { 42 }
        }

        assertAll(
            { assertEquals(42, result) },
            { assertTrue(messages.any { it == "Calling doWork()" }, "entry log: $messages") },
            { assertTrue(messages.any { it.startsWith("doWork returned: 42") }, "exit log: $messages") },
        )
    }
}

/** Supplies a [LoggingContext] context argument positionally. */
private suspend fun <T> withLogging(
    loggingContext: LoggingContext,
    block: suspend context(LoggingContext) () -> T,
): T = block(loggingContext)

/** Supplies a [MeterRegistry] context argument positionally. */
private suspend fun <T> withRegistry(
    registry: MeterRegistry,
    block: suspend context(MeterRegistry) () -> T,
): T = block(registry)
