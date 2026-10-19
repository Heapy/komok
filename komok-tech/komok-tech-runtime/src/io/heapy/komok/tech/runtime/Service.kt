package io.heapy.komok.tech.runtime

import kotlin.reflect.KClass

/**
 * Contributes a free-floating param-wired binding to the compiled graph.
 *
 * Function parameters or primary-constructor parameters are resolved by exact produced
 * type. `value`/`bind` expose a provider under a supertype.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Service(
    val value: KClass<*> = Unit::class,
    val bind: KClass<*> = Unit::class,
)
