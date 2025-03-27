package io.heapy.komok.tech.decorators.lib

import kotlin.reflect.KClass

/**
 * Indicate what context parameters expected for decorated function to work properly.
 * Like TransactionContext for TransactionalDecorator,
 * UserContext for AuthorizationDecorator, etc.
 */
annotation class DecoratorContext(
    vararg val types: KClass<*>,
)
