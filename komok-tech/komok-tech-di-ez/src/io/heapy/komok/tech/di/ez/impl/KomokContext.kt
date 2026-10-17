package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Binding
import io.heapy.komok.tech.di.ez.api.Context
import io.heapy.komok.tech.di.ez.api.GenericKey
import io.heapy.komok.tech.di.ez.api.Key
import io.heapy.komok.tech.di.ez.api.Provider

internal class KomokContext(
    private val definitions: Map<Key, Binding<*>>,
) : Context {
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: GenericKey<T>): T {
        val executionContext = object : ExecutionContext {
            override val definitions: Map<Key, Binding<*>> = this@KomokContext.definitions
            override val stack: MutableList<Key> = mutableListOf()
            override val instances: MutableMap<Key, Any?> = mutableMapOf()
        }

        return if (key.isProvider()) {
            object : Provider<Any?> {
                override fun get(): Any? {
                    return executionContext.createType(
                        key = GenericKey<Any?>(type = key.type.arguments.first().type!!),
                    )
                }
            }
        } else {
            executionContext.createType(key)
        } as T
    }
}
