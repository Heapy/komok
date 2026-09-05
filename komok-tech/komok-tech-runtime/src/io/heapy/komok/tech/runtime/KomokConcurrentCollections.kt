package io.heapy.komok.tech.runtime

internal expect class KomokConcurrentMap<K : Any, V : Any>() {
    operator fun get(key: K): V?

    fun put(
        key: K,
        value: V,
    )

    fun putIfAbsent(
        key: K,
        value: V,
    ): V?
}

internal expect class KomokConcurrentSet<T : Any>() {
    fun add(value: T): Boolean

    fun snapshot(): Set<T>
}

internal expect class KomokConcurrentList<T : Any>() {
    fun add(value: T): Boolean

    fun snapshot(): List<T>
}
