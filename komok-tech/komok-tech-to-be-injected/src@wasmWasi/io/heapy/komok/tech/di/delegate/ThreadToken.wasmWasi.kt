package io.heapy.komok.tech.di.delegate

private val singleThreadToken = Any()

internal actual fun currentThreadToken(): Any = singleThreadToken
