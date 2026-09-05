package io.heapy.komok.tech.runtime

import kotlin.reflect.KClass

/**
 * Imports provider-shaped functions from explicitly mentioned classes into the binding index.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProvidersFrom(
    vararg val value: KClass<*>,
)
