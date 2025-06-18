package io.heapy.komok.tech.di.ez.api

interface Context {
    fun <T> get(key: GenericKey<T>): T
}

inline fun <reified T> Context.get(): T {
    return get(genericKey<T>())
}
