package io.heapy.komok.tech.logging

/**
 * Only passes events at or above [minLevel].
 */
class LevelFilter(private val minLevel: Level) : LogHandlerWrapper {
    override fun wrap(next: LogHandler): LogHandler = LogHandler { event ->
        if (event.level.ordinal >= minLevel.ordinal) next.handle(event)
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

    override fun wrap(next: LogHandler): LogHandler = LogHandler { event ->
        val matches = if (matchExact) {
            event.loggerName == prefix
        } else {
            event.loggerName == prefix || event.loggerName.startsWith("$prefix.")
        }
        if (matches) next.handle(event)
    }

    override fun toString(): String = "LoggerNameFilter($prefix)"
}

/**
 * Generic predicate filter.
 */
class PredicateFilter(
    private val predicate: EventFilter,
) : LogHandlerWrapper {

    override fun wrap(next: LogHandler): LogHandler = LogHandler { event ->
        if (predicate(event)) next.handle(event)
    }

    override fun toString(): String = "PredicateFilter"
}

/**
 * Sampling filter — only passes every Nth event.
 */
class SamplingFilter(
    private val rate: Int,
) : LogHandlerWrapper {

    private val counter = java.util.concurrent.atomic.AtomicLong(0)

    override fun wrap(next: LogHandler): LogHandler = LogHandler { event ->
        if (counter.incrementAndGet() % rate == 0L) next.handle(event)
    }

    override fun toString(): String = "SamplingFilter(1/$rate)"
}
