@file:Suppress("NOTHING_TO_INLINE")

package io.heapy.komok.tech.lazy.const

import java.util.function.Function
import java.util.function.IntFunction
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Kotlin property delegate for [StableValue.supplier]
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     val myValue by lazyConst { "myValue" }
 * }
 * ```
 */
inline fun <T> lazyConst(
    noinline initializer: () -> T,
): ReadOnlyProperty<Any, T> =
    LazyConstSupplierProperty(
        initializer = initializer,
    )

@PublishedApi
internal class LazyConstSupplierProperty<T>(
    initializer: () -> T,
) : ReadOnlyProperty<Any, T> {
    private val value = StableValue.supplier(initializer)

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): T {
        return value.get()
    }
}

/**
 * Kotlin wrapper for [StableValue.list]
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     val myList by lazyList(10) { "myValue$it" }
 * }
 * ```
 */
inline fun <E> lazyList(
    size: Int,
    mapper: IntFunction<E>,
): List<E> = StableValue.list(size, mapper)

/**
 * Kotlin wrapper for [StableValue.map]
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     val myMap by lazyMap(setOf("a", "b", "c")) { it.uppercase() }
 * }
 * ```
 */
inline fun <K, V> lazyMap(
    value: Set<K>,
    underlying: Function<K, V>
): Map<K, V> = StableValue.map(value, underlying)
