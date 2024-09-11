package io.heapy.komok.tech.di.ez.dsl

import io.heapy.komok.tech.di.ez.api.ProviderBinding
import io.heapy.komok.tech.di.ez.api.Binder
import io.heapy.komok.tech.di.ez.api.ModuleDSL
import io.heapy.komok.tech.di.ez.api.genericKey
import kotlin.reflect.KFunction

@ModuleDSL
inline fun <reified I> Binder.provide(
    provider: KFunction<I>,
) {
    contribute(
        ProviderBinding(
            key = genericKey<I>(),
            provider = provider,
            source = source,
        )
    )
}
