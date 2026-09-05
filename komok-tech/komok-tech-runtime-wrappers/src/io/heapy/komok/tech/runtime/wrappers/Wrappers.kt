package io.heapy.komok.tech.runtime.wrappers

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.Logger
import io.heapy.komok.tech.logging.LoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

/**
 * Produced-value wrapper helpers shared by generated `*_KomokDecorated` classes. No
 * reflection: method identity arrives as a generated static [MethodMeta]. Each helper
 * wraps a single `suspend` service-method call. When several apply, the generated nesting
 * fixes the order: [logCall] -> [metricCall] -> [retryCall] -> delegate.
 *
 * Wrapper context is **never static**: a helper that needs context declares it as a
 * context parameter ([LoggingContext] for `@Log`, [MeterRegistry] for `@Metric`), the
 * decorated method declares the same context parameters, and the caller provides them at
 * the call site — exactly like providers receive `KomokContext`.
 */

/** `@Log`: logs entry, timed exit (result/duration), and failure, under the caller-provided [LoggingContext]. */
context(_: LoggingContext)
suspend fun <T> logCall(
    logger: Logger,
    meta: MethodMeta,
    level: Level,
    includeResult: Boolean,
    includeDuration: Boolean,
    call: suspend () -> T,
): T {
    logger.at(level) { "Calling ${meta.name}()" }
    val start = TimeSource.Monotonic.markNow()
    try {
        val result = call()
        val ms = start.elapsedNow().inWholeMilliseconds
        logger.at(level) {
            buildString {
                append(meta.name)
                append(" returned")
                if (includeResult) {
                    append(": ")
                    append(result)
                }
                if (includeDuration) {
                    append(", took ")
                    append(ms)
                    append("ms")
                }
            }
        }
        return result
    } catch (e: Throwable) {
        logger.error(e) { "${meta.name} failed" }
        throw e
    }
}

/** `@Metric`: times the call against the caller-provided [MeterRegistry], tagging the outcome. */
context(registry: MeterRegistry)
suspend fun <T> metricCall(
    meta: MethodMeta,
    name: String,
    call: suspend () -> T,
): T {
    val metricName = name.ifEmpty { "${meta.owner}.${meta.name}" }
    val sample = Timer.start(registry)
    var outcome = "success"
    try {
        return call()
    } catch (e: Throwable) {
        outcome = "failure"
        throw e
    } finally {
        sample.stop(registry.timer(metricName, "outcome", outcome))
    }
}

/** `@Retry`: re-invokes the call up to [maxAttempts] times; never retries cancellation. */
suspend fun <T> retryCall(
    meta: MethodMeta,
    maxAttempts: Int,
    delayMillis: Long,
    call: suspend () -> T,
): T {
    var attempt = 1
    while (true) {
        try {
            return call()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (attempt >= maxAttempts) {
                throw e
            }
            attempt++
            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }
}

context(_: LoggingContext)
private inline fun Logger.at(
    level: Level,
    throwable: Throwable? = null,
    crossinline msg: () -> String,
) {
    when (level) {
        Level.TRACE -> trace(throwable) { msg() }
        Level.DEBUG -> debug(throwable) { msg() }
        Level.INFO -> info(throwable) { msg() }
        Level.WARN -> warn(throwable) { msg() }
        Level.ERROR -> error(throwable) { msg() }
        Level.OFF -> {}
    }
}
