package io.heapy.komok.tech.logging

import kotlin.time.Instant

/**
 * Immutable log event — the unit of data that flows through the handler pipeline.
 *
 * NOT a data class. Lazy evaluation of expensive fields avoids unnecessary computation.
 * [mdc] is snapshotted at creation time from the current [LoggingContext].
 */
class LogEvent(
    val level: Level,
    val loggerName: String,
    val message: String,
    val throwable: ThrowableProxy?,
    val timestamp: Instant,
    val mdc: Mdc,
) {
    val threadName: String by lazy { Thread.currentThread().name }

    /**
     * Force lazy field evaluation — call before handing off to async handler.
     */
    fun prepareForDeferredProcessing() {
        val _ = threadName
    }

    override fun toString(): String =
        "LogEvent(level=$level, logger=$loggerName, message=$message)"
}
