package io.heapy.komok.tech.logging

/**
 * Adds a fixed MDC entry to every event passing through.
 */
class MdcEnricher<T : Any>(
    private val key: Mdc.Key<T>,
    private val value: T,
) : LogHandlerWrapper {

    override fun wrap(next: LogHandler): LogHandler =
        object : LogHandler {
            override fun handle(event: LogEvent) {
                next.handle(
                    LogEvent(
                        level = event.level,
                        loggerName = event.loggerName,
                        message = event.message,
                        throwable = event.throwable,
                        timestamp = event.timestamp,
                        mdc = event.mdc.with(key, value),
                    ),
                )
            }

            override fun isEnabled(
                level: Level,
                loggerName: String,
            ): Boolean =
                next.isEnabled(level, loggerName)
        }

    override fun toString(): String = "MdcEnricher(${key.name}=$value)"
}
