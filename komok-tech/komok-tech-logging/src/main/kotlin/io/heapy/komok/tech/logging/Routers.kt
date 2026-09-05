package io.heapy.komok.tech.logging

/**
 * Generic router — routes by a discriminator function.
 */
class Router<K>(
    private val discriminator: (LogEvent) -> K,
    private val routes: Map<K, LogHandler>,
    private val fallback: LogHandler = LogHandler.noop(),
) : LogHandler {

    override fun handle(event: LogEvent) {
        val key = discriminator(event)
        val handler = routes[key] ?: fallback
        handler.handle(event)
    }

    override fun toString(): String = "Router(${routes.size} routes)"
}

/**
 * Logger-name prefix router — the most common "sifting" pattern.
 * Routes are checked in declaration order; first matching prefix wins.
 */
class LoggerRouter(
    private val routes: List<Pair<String, LogHandler>>,
    private val fallback: LogHandler = LogHandler.noop(),
) : LogHandler {

    override fun handle(event: LogEvent) {
        for ([prefix, handler] in routes) {
            if (event.loggerName == prefix || event.loggerName.startsWith("$prefix.")) {
                handler.handle(event)
                return
            }
        }
        fallback.handle(event)
    }

    override fun toString(): String =
        "LoggerRouter(${routes.joinToString { it.first }})"
}

/**
 * MDC-key router — route by the value of a typed MDC key.
 */
class MdcRouter<T : Any>(
    private val key: Mdc.Key<T>,
    private val routes: Map<T, LogHandler>,
    private val fallback: LogHandler = LogHandler.noop(),
) : LogHandler {

    override fun handle(event: LogEvent) {
        val value = event.mdc[key]
        val handler = value?.let { routes[it] } ?: fallback
        handler.handle(event)
    }

    override fun toString(): String = "MdcRouter(${key.name}, ${routes.size} routes)"
}

/**
 * Fan-out with filtering — send to multiple handlers,
 * each with its own predicate.
 */
class Tee(
    private val branches: List<Pair<EventFilter, LogHandler>>,
) : LogHandler {

    override fun handle(event: LogEvent) {
        for ([predicate, handler] in branches) {
            if (predicate(event)) {
                handler.handle(event)
            }
        }
    }

    override fun toString(): String = "Tee(${branches.size} branches)"
}
