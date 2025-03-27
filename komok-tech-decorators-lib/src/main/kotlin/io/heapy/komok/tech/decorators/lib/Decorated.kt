package io.heapy.komok.tech.decorators.lib

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Decorated(
    val type: KClass<Decorator>,
)
