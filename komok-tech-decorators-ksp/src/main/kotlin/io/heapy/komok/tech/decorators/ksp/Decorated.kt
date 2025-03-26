package io.heapy.komok.tech.decorators.ksp

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Decorated(
    val type: KClass<Decorator>,
)
