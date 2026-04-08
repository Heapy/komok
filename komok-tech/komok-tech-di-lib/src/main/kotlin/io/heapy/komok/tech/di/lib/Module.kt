package io.heapy.komok.tech.di.lib

/**
 * Marks a class as a module.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Module

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "This was not used as a real DslMarker",
)
@DslMarker
annotation class KomokModuleDsl
