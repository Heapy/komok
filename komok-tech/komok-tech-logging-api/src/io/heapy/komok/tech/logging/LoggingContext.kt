package io.heapy.komok.tech.logging

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Immutable logging context — flows through Kotlin context parameters.
 *
 * Carries two things:
 * 1. The [handler] pipeline (built once from DSL configuration).
 * 2. The current [mdc] values (derived per-scope, e.g., per-request).
 *
 * **No global state.** Context flows exclusively through Kotlin context parameters.
 */
class LoggingContext(
    val handler: LogHandler,
    val mdc: Mdc = Mdc.Empty,
    val now: () -> Instant = { Clock.System.now() },
) {
    fun withMdc(mdc: Mdc): LoggingContext =
        LoggingContext(
            handler = handler,
            mdc = this.mdc + mdc,
            now = now,
        )

    fun <T : Any> withMdc(
        key: Mdc.Key<T>,
        value: T,
    ): LoggingContext =
        LoggingContext(
            handler = handler,
            mdc = this.mdc.with(
                key,
                value,
            ),
            now = now,
        )

    inline fun withMdc(block: MdcBuilder.() -> Unit): LoggingContext =
        withMdc(buildMdc(block))

    @PublishedApi
    internal fun dispatch(event: LogEvent) {
        handler.handle(event)
    }

    override fun toString(): String =
        "LoggingContext(mdc=$mdc)"
}

val EmptyLoggingContext = LoggingContext(LogHandler.noop())

inline fun <T> withEmptyLoggingContext(
    block: context(LoggingContext) () -> T,
): T = block(EmptyLoggingContext)

context(loggingContext: LoggingContext)
inline val loggingContext: LoggingContext
    get() = loggingContext

context(outer: LoggingContext)
inline fun <T> withMdc(
    mdc: Mdc,
    block: context(LoggingContext) () -> T,
): T {
    val derived = outer.withMdc(mdc)
    return block(derived)
}

context(outer: LoggingContext)
inline fun <R : Any, T> withMdc(
    key: Mdc.Key<R>,
    value: R,
    block: context(LoggingContext) () -> T,
): T {
    val derived = outer.withMdc(
        key,
        value,
    )
    return block(derived)
}

context(outer: LoggingContext)
inline fun <T> withMdc(
    noinline buildBlock: MdcBuilder.() -> Unit,
    block: context(LoggingContext) () -> T,
): T {
    val derived = outer.withMdc(buildBlock)
    return block(derived)
}
