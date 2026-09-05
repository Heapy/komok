package io.heapy.komok.tech.logging

import java.util.concurrent.atomic.AtomicReference

/**
 * Manages [LoggingContext] with atomic hot-reload support.
 *
 * Log calls read the current context atomically via [current].
 * Use [reload] to swap the configuration atomically.
 */
class ConfigManager(
    initialContext: LoggingContext,
) {
    private val ref = AtomicReference(initialContext)

    val current: LoggingContext get() = ref.get()

    fun reload(newContext: LoggingContext) {
        ref.set(newContext)
    }
}
