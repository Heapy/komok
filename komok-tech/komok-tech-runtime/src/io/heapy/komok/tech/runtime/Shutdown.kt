package io.heapy.komok.tech.runtime

/**
 * Marks a `suspend` function inside a [io.heapy.komok.tech.runtime.Module] as a shutdown hook.
 *
 * Shutdown hooks run in strictly reverse [Startup] order, gracefully, awaiting
 * (`join`) each one.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shutdown
