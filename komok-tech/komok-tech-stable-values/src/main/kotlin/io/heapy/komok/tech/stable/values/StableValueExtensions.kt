@file:Suppress("NOTHING_TO_INLINE")

package io.heapy.komok.tech.stable.values

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
 *     val myValue by stableValue { "myValue" }
 * }
 * ```
 */
inline fun <T> stableValue(
    noinline initializer: () -> T,
): ReadOnlyProperty<Any, T> =
    StableValueSupplierProperty(
        initializer = initializer,
    )

@PublishedApi
internal class StableValueSupplierProperty<T>(
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
 *     val myList by stableList(10) { "myValue$it" }
 * }
 * ```
 */
inline fun <E> stableList(
    size: Int,
    mapper: IntFunction<E>,
): List<E> = StableValue.list(size, mapper)

/**
 * Kotlin wrapper for [StableValue.map]
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     val myMap by stableMap(setOf("a", "b", "c")) { it.uppercase() }
 * }
 * ```
 */
inline fun <K, V> stableMap(
    value: Set<K>,
    underlying: Function<K, V>
): Map<K, V> = StableValue.map(value, underlying)

/**
 * Kotlin wrapper for [StableValue.function]
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     val myFunction by stableFunction(setOf("a", "b", "c")) { it.uppercase() }
 * }
 * ```
 */
inline fun <T, R> stableFunction(
    value: Set<T>,
    underlying: Function<T, R>
): Function<T, R> = StableValue.function(value, underlying)

/**
 * Kotlin wrapper for [StableValue.intFunction]
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     val myIntFunction by stableIntFunction(42) { it * 10 }
 * }
 * ```
 */
inline fun <R> stableIntFunction(
    size: Int,
    underlying: IntFunction<R>,
): IntFunction<R> = StableValue.intFunction(size, underlying)
