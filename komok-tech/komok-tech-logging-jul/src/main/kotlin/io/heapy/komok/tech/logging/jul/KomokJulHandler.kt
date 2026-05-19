package io.heapy.komok.tech.logging.jul

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.logging.Mdc
import io.heapy.komok.tech.logging.ThrowableProxy
import kotlin.time.Instant
import java.util.logging.Handler
import java.util.logging.LogRecord

/**
 * java.util.logging Handler that bridges JUL logging calls to Komok Logging.
 *
 * **No global state.** Receives a fixed [LoggingContext] at construction time.
 *
 * Usage:
 * ```
 * val julHandler = KomokJulHandler(loggingContext)
 * java.util.logging.Logger.getLogger("").addHandler(julHandler)
 * ```
 */
class KomokJulHandler(
    private val context: LoggingContext,
) : Handler() {
    override fun publish(record: LogRecord?) {
        record ?: return

        val event = LogEvent(
            level = mapLevel(record.level),
            loggerName = record.loggerName ?: "unknown",
            message = record.message ?: "",
            throwable = record.thrown?.let { ThrowableProxy.create(it) },
            timestamp = Instant.fromEpochMilliseconds(record.millis),
            mdc = Mdc.Empty,
        )

        context.handler.handle(event)
    }

    override fun flush() {}

    override fun close() {}

    private fun mapLevel(level: java.util.logging.Level): Level = when {
        level.intValue() >= java.util.logging.Level.SEVERE.intValue() -> Level.ERROR
        level.intValue() >= java.util.logging.Level.WARNING.intValue() -> Level.WARN
        level.intValue() >= java.util.logging.Level.INFO.intValue() -> Level.INFO
        level.intValue() >= java.util.logging.Level.FINE.intValue() -> Level.DEBUG
        else -> Level.TRACE
    }
}
