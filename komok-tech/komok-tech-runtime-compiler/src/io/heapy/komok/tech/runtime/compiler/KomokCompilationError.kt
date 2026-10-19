package io.heapy.komok.tech.runtime.compiler

/** Expected source error, reported through Kotlin's diagnostic API instead of crashing IR. */
internal class KomokCompilationError(message: String) : RuntimeException(message)

internal inline fun komokRequire(value: Boolean, message: () -> String) {
    if (!value) throw KomokCompilationError(message())
}

internal fun komokError(message: String): Nothing = throw KomokCompilationError(message)
