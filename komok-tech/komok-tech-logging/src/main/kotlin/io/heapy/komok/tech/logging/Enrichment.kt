package io.heapy.komok.tech.logging

/**
 * Adds a fixed MDC entry to every event passing through.
 */
class MdcEnricher<T : Any>(
    private val key: Mdc.Key<T>,
    private val value: T,
) : LogHandlerWrapper {

    override fun wrap(next: LogHandler): LogHandler = LogHandler { event ->
        val enriched = LogEvent(
            level = event.level,
            loggerName = event.loggerName,
            message = event.message,
            throwable = event.throwable,
            timestamp = event.timestamp,
            mdc = event.mdc.with(key, value),
        )
        next.handle(enriched)
    }

    override fun toString(): String = "MdcEnricher(${key.name}=$value)"
}
