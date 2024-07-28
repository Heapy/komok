package io.heapy.komok.infra.di

import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Functional type that represents a module.
 * It may be a functional interface, but it tends to implement module as classes,
 * which introduces more unnecessary boilerplate.
 */
typealias ModuleBuilder = Binder.() -> Unit

interface ModuleProvider {
    fun module(): Module
}

interface Module {
    /**
     * Source of the module (class fqn, or file and variable name).
     * Library will enforce uniqueness
     */
    val source: String

    /**
     * Dependencies of this module
     */
    val dependencies: List<ModuleProvider>

    /**
     * Bindings defined in this module
     */
    val bindings: List<Binding<*>>
}

/**
 * Binder used to collect all [Binding]s and create context from them.
 *
 * @author Ruslan Ibrahimau
 */
interface Binder {
    val source: String

    @ModuleDSL
    fun dependency(module: ModuleProvider)

    @ModuleDSL
    fun contribute(binding: Binding<*>)
}

@DslMarker
annotation class ModuleDSL

sealed interface Key {
    val type: KType
}

data class GenericKey<T>(
    override val type: KType,
) : Key {
    override fun toString(): String = type.toString()
}

inline fun <reified T> genericKey(): GenericKey<T> {
    return GenericKey(typeOf<T>())
}

interface Context {
    fun <T> get(key: Key): T
}

interface Provider<out T> {
    fun get(): T
}
