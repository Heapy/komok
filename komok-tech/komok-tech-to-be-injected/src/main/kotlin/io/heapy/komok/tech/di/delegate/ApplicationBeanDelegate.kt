package io.heapy.komok.tech.di.delegate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate that provides instance of [MutableBean].
 */
internal class ApplicationBeanDelegate<V> internal constructor(
    initializer: () -> V,
) : ReadOnlyProperty<Any, MutableBean<V>> {
    private var initializer: (() -> V)? = initializer
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    // final field is required to enable safe publication of constructed instance
    private val lock = this

    /**
     * Returns the initializer wrapped in [MutableBean].
     * If the value is not initialized, it will be initialized by calling the initializer.
     * If the value is already initialized, the current value will be returned.
     *
     * Uses classic DCL, implementation similar to [SynchronizedLazyImpl].
     */
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): MutableBean<V> {
        val try1 = _value
        if (try1 !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return try1 as MutableBean<V>
        }

        return synchronized(lock) {
            val try2 = _value
            if (try2 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                try2 as MutableBean<V>
            } else {
                val typedValue = MutableBean(
                    initializer = initializer!!,
                    name = "${thisRef::class.qualifiedName}.${property.name}",
                )
                _value = typedValue
                initializer = null
                typedValue
            }
        }
    }
}
