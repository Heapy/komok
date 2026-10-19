package io.heapy.komok.tech.runtime

/** Versioned binary metadata consumed by the compiler when wiring downstream graphs. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class KomokBindingIndex(
    val bindings: Array<String> = [],
    val version: Int = 1,
)
