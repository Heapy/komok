package io.heapy.komok.infra.di

class MultiMap<T> {
    private val map = mutableMapOf<String, MutableList<T>>()

    fun put(
        key: String,
        value: T,
    ) {
        map
            .getOrPut(key) { mutableListOf() }
            .add(value)
    }

    fun get(key: String): List<T> {
        return map[key] as List<T>?
            ?: emptyList()
    }
}
