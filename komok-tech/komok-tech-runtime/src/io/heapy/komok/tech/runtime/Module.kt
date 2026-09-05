package io.heapy.komok.tech.runtime

/**
 * Marks a class as a Komok runtime module.
 *
 * The KSP processor generates a `${Module}_Komok` subclass that wraps each
 * provider-shape function (`context(_: KomokContext) suspend fun` without value
 * parameters) in a runtime binding boundary, plus `${Module}Keys`, fork-scope and
 * graph handles. Cross-module dependencies are constructor parameters referencing
 * other `@Module` classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Module
