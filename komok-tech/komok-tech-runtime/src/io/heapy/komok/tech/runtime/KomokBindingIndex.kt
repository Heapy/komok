package io.heapy.komok.tech.runtime

/**
 * Binary carrier read by KSP from dependency jars to merge bindings across compilations.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class KomokBindingIndex(
    val bindings: Array<String> = [],
    val setContributions: Array<String> = [],
    val setConsumers: Array<String> = [],
)
