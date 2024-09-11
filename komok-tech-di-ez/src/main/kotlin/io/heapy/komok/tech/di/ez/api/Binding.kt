package io.heapy.komok.tech.di.ez.api

import kotlin.reflect.KFunction

sealed interface Binding<T> {
    val key: GenericKey<T>
    val source: String
}

data class ProviderBinding<T>(
    override val key: GenericKey<T>,
    @PublishedApi internal val provider: KFunction<T>,
    override val source: String,
) : Binding<T>
