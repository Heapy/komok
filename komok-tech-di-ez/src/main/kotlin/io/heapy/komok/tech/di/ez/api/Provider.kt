package io.heapy.komok.tech.di.ez.api

interface Provider<out T> {
    fun get(): T
}
