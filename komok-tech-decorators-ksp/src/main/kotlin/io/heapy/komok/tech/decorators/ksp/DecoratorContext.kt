package io.heapy.komok.tech.decorators.ksp

import kotlin.reflect.KClass

/**
 * Indicate what function parameters expected for decorated function to work properly.
 * Like TransactionContext, SecurityContext, UserContext, etc.
 */
annotation class DecoratorContext(
    vararg val types: KClass<*>,
)
