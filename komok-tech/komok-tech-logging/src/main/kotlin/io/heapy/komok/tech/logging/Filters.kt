package io.heapy.komok.tech.logging

import java.util.concurrent.atomic.AtomicLong

/**
 * Only passes events at or above [minLevel].
 */
class LevelFilter(private val minLevel: Level) : LogHandlerWrapper {
    override fun wrap(next: LogHandler): LogHandler =
        object : LogHandler {
            override fun handle(event: LogEvent) {
                if (event.level.ordinal >= minLevel.ordinal) next.handle(event)
            }

            override fun isEnabled(
                level: Level,
                loggerName: String,
            ): Boolean =
                level.ordinal >= minLevel.ordinal && next.isEnabled(level, loggerName)
        }

    override fun toString(): String = "LevelFilter(>=$minLevel)"
}

/**
 * Only passes events whose logger name starts with [prefix].
 */
class LoggerNameFilter(
    private val prefix: String,
    private val matchExact: Boolean = false,
) : LogHandlerWrapper {

    private fun matches(loggerName: String): Boolean =
        if (matchExact) {
            loggerName == prefix
        } else {
            loggerName == prefix || loggerName.startsWith("$prefix.")
        }

    override fun wrap(next: LogHandler): LogHandler =
        object : LogHandler {
            override fun handle(event: LogEvent) {
                if (matches(event.loggerName)) next.handle(event)
            }

            override fun isEnabled(
                level: Level,
                loggerName: String,
            ): Boolean =
                matches(loggerName) && next.isEnabled(level, loggerName)
        }

    override fun toString(): String = "LoggerNameFilter($prefix)"
}

/**
 * Generic predicate filter.
 */
class PredicateFilter(
    private val predicate: EventFilter,
) : LogHandlerWrapper {

    override fun wrap(next: LogHandler): LogHandler =
        object : LogHandler {
            override fun handle(event: LogEvent) {
                if (predicate(event)) next.handle(event)
            }

            override fun isEnabled(
                level: Level,
                loggerName: String,
            ): Boolean =
                next.isEnabled(level, loggerName)
        }

    override fun toString(): String = "PredicateFilter"
}

/**
 * Sampling filter — only passes every Nth event.
 */
class SamplingFilter(
    private val rate: Int,
) : LogHandlerWrapper {

    private val counter = AtomicLong(0)

    override fun wrap(next: LogHandler): LogHandler =
        object : LogHandler {
            override fun handle(event: LogEvent) {
                if (counter.incrementAndGet() % rate == 0L) next.handle(event)
            }

            override fun isEnabled(
                level: Level,
                loggerName: String,
            ): Boolean =
                next.isEnabled(level, loggerName)
        }

    override fun toString(): String = "SamplingFilter(1/$rate)"
}
