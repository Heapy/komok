package io.heapy.komok.tech.di.ez.api

import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
