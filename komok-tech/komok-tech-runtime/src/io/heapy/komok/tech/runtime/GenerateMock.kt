package io.heapy.komok.tech.runtime

import kotlin.reflect.KClass

/**
 * Requests a generated compile-time mock/spy for types that are not otherwise discovered
 * as decoratable service return types.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class GenerateMock(
    vararg val value: KClass<*>,
)
