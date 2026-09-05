package io.heapy.komok.tech.runtime

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

internal actual class KomokConcurrentMap<K : Any, V : Any> actual constructor() {
    private val delegate = ConcurrentHashMap<K, V>()

    actual operator fun get(key: K): V? =
        delegate[key]

    actual fun put(
        key: K,
        value: V,
    ) {
        delegate[key] = value
    }

    actual fun putIfAbsent(
        key: K,
        value: V,
    ): V? =
        delegate.putIfAbsent(key, value)
}

internal actual class KomokConcurrentSet<T : Any> actual constructor() {
    private val delegate = ConcurrentHashMap.newKeySet<T>()

    actual fun add(value: T): Boolean =
        delegate.add(value)

    actual fun snapshot(): Set<T> =
        delegate.toSet()
}

internal actual class KomokConcurrentList<T : Any> actual constructor() {
    private val delegate = ConcurrentLinkedQueue<T>()

    actual fun add(value: T): Boolean =
        delegate.add(value)

    actual fun snapshot(): List<T> =
        delegate.toList()
}
