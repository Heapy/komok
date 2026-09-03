package io.heapy.komok.tech.di.delegate

import kotlin.properties.ReadOnlyProperty

/**
 * Creates a property delegate for a bean.
 *
 * ```kotlin
 * class MyModule {
 *     val myBean by bean {
 *         MyBean()
 *     }
 * }
 * ```
 */
fun <V> bean(
    initializer: () -> V,
): ReadOnlyProperty<Any, MutableBean<V>> =
    ApplicationBeanDelegate(
        initializer = initializer,
    )
