package io.heapy.komok.tech.logging

/**
 * Terminal handler — receives a [LogEvent] and does something with it.
 * Appenders, routers, and composite handlers all implement this.
 */
fun interface LogHandler {
    fun handle(event: LogEvent)

    /**
     * Whether an event of [level] from [loggerName] can reach any appender.
     *
     * Callers use this to skip building the event, so an implementation must
     * not have side effects and must answer `true` whenever the decision needs
     * the event itself.
     */
    fun isEnabled(
        level: Level,
        loggerName: String,
    ): Boolean = true

    companion object {
        fun noop(): LogHandler =
            NoopHandler

        fun composite(handlers: List<LogHandler>): LogHandler =
            when (handlers.size) {
                0 -> noop()
                1 -> handlers.first()
                else -> CompositeHandler(handlers.toList())
            }

        fun composite(vararg handlers: LogHandler): LogHandler =
            composite(handlers.toList())
    }
}

private class CompositeHandler(
    private val handlers: List<LogHandler>,
) : LogHandler {
    override fun handle(event: LogEvent) {
        for (handler in handlers) {
            handler.handle(event)
        }
    }

    override fun isEnabled(
        level: Level,
        loggerName: String,
    ): Boolean =
        handlers.any { it.isEnabled(level, loggerName) }

    override fun toString(): String =
        "Composite(${handlers.size} handlers)"
}

/**
 * Middleware — wraps one [LogHandler] to produce another.
 */
fun interface LogHandlerWrapper {
    fun wrap(next: LogHandler): LogHandler

    companion object {
        fun identity(): LogHandlerWrapper =
            LogHandlerWrapper { it }
    }
}

private object NoopHandler : LogHandler {
    override fun handle(event: LogEvent) {}

    override fun isEnabled(
        level: Level,
        loggerName: String,
    ): Boolean = false

    override fun toString(): String =
        "Noop"
}

infix fun LogHandlerWrapper.andThen(inner: LogHandlerWrapper): LogHandlerWrapper =
    LogHandlerWrapper { next -> this.wrap(inner.wrap(next)) }

fun LogHandler.wrappedBy(vararg wrappers: LogHandlerWrapper): LogHandler =
    wrappers.foldRight(this) { wrapper, handler -> wrapper.wrap(handler) }

fun LogHandler.wrappedBy(wrappers: List<LogHandlerWrapper>): LogHandler =
    wrappers.foldRight(this) { wrapper, handler -> wrapper.wrap(handler) }
