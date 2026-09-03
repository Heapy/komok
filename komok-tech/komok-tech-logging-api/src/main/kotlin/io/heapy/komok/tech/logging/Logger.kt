package io.heapy.komok.tech.logging

import kotlin.reflect.KClass
import kotlin.time.Clock

/**
 * A logger is just a name — it carries no configuration, no appender references.
 * It's a zero-cost label that delegates all work to the [LoggingContext]
 * flowing through context parameters.
 */
@JvmInline
value class Logger @PublishedApi internal constructor(
    val name: String,
) {
    context(ctx: LoggingContext)
    inline fun trace(
        throwable: Throwable? = null,
        msg: () -> String,
    ) =
        log(
            level = Level.TRACE,
            throwable = throwable,
            msg = msg,
        )

    context(_: LoggingContext)
    inline fun debug(
        throwable: Throwable? = null,
        msg: () -> String,
    ) =
        log(
            level = Level.DEBUG,
            throwable = throwable,
            msg = msg,
        )

    context(_: LoggingContext)
    inline fun info(
        throwable: Throwable? = null,
        msg: () -> String,
    ) =
        log(
            level = Level.INFO,
            throwable = throwable,
            msg = msg,
        )

    context(_: LoggingContext)
    inline fun warn(
        throwable: Throwable? = null,
        msg: () -> String,
    ) =
        log(
            level = Level.WARN,
            throwable = throwable,
            msg = msg,
        )

    context(_: LoggingContext)
    inline fun error(
        throwable: Throwable? = null,
        msg: () -> String,
    ) =
        log(
            level = Level.ERROR,
            throwable = throwable,
            msg = msg,
        )

    @PublishedApi
    context(_: LoggingContext)
    internal inline fun log(
        level: Level,
        throwable: Throwable?,
        msg: () -> String,
    ) {
        val event = LogEvent(
            level = level,
            loggerName = name,
            message = msg(),
            throwable = throwable?.let { ThrowableProxy.create(it) },
            timestamp = Clock.System.now(),
            mdc = loggingContext.mdc,
        )
        loggingContext.dispatch(event)
    }

    override fun toString(): String =
        "Logger($name)"
}

inline fun <reified T> logger(): Logger =
    Logger(
        T::class.qualifiedName
            ?: T::class.simpleName
            ?: "Anonymous",
    )

fun logger(name: String): Logger =
    Logger(name)

fun logger(clazz: KClass<*>): Logger =
    Logger(
        clazz.qualifiedName
            ?: clazz.simpleName
            ?: "Anonymous",
    )

fun Any.logger(): Logger =
    Logger(
        this::class.qualifiedName
            ?: this::class.simpleName
            ?: "Anonymous",
    )

@Suppress("NOTHING_TO_INLINE")
inline fun getLogger(className: String): Logger {
    val name = when {
        className.contains("Kt$") -> className.substringBefore("Kt$")
        className.contains("$") -> className.substringBefore("$")
        else -> className
    }

    return Logger(name)
}

open class JvmLogger {
    protected val log: Logger = getLogger(this.javaClass.name)
}
