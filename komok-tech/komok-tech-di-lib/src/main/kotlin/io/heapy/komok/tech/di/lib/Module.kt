package io.heapy.komok.tech.di.lib

/**
 * Marks a class as a module.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Module

@DslMarker
annotation class KomokModuleDsl
