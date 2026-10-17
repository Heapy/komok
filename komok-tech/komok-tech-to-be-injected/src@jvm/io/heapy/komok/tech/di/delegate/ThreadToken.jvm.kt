package io.heapy.komok.tech.di.delegate

internal actual fun currentThreadToken(): Any = Thread.currentThread()
