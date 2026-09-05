package io.heapy.komok.tech.runtime

import kotlin.reflect.KClass

/**
 * Contributes a free-floating param-wired binding to the generated synthetic module.
 *
 * Function parameters or primary-constructor parameters are resolved by exact produced
 * type. `value`/`bind` are reserved for binding under a supertype; v1 support is added
 * incrementally by the KSP processor.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Service(
    val value: KClass<*> = Unit::class,
    val bind: KClass<*> = Unit::class,
)
