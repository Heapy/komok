package io.heapy.komok.tech.di.delegate

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate that provides an instance of [MutableBean].
 */
@OptIn(ExperimentalAtomicApi::class)
internal class ApplicationBeanDelegate<V> internal constructor(
    private val initializer: () -> V,
) : ReadOnlyProperty<Any, MutableBean<V>> {
    private val bean = AtomicReference<MutableBean<V>?>(null)

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): MutableBean<V> =
        bean.load()
            ?: buildAndReturn(
                thisRef = thisRef,
                property = property,
            )

    private fun buildAndReturn(
        thisRef: Any,
        property: KProperty<*>,
    ): MutableBean<V> {
        val candidate = MutableBean(
            initializer = initializer,
            name = "${thisRef::class.simpleName ?: "<anonymous>"}.${property.name}",
        )

        val oldValue = bean
            .compareAndExchange(
                expectedValue = null,
                newValue = candidate,
            )

        return oldValue ?: candidate
    }
}
