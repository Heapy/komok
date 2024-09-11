package io.heapy.komok.tech.di.ez.dsl

import io.heapy.komok.tech.di.ez.api.ProviderBinding
import io.heapy.komok.tech.di.ez.api.Binder
import io.heapy.komok.tech.di.ez.api.ModuleDSL
import io.heapy.komok.tech.di.ez.api.genericKey
import io.heapy.komok.tech.di.ez.impl.asProvider

@ModuleDSL
inline fun <reified I> Binder.provideInstance(
    instance: I,
) {
    contribute(
        ProviderBinding(
            key = genericKey<I>(),
            provider = instance.asProvider(),
            source = source,
        )
    )
}
