package io.heapy.komok.tech.di.delegate

internal actual fun logBeanInfo(message: String) = Unit

internal actual fun logBeanError(
    message: String,
    throwable: Throwable,
) = Unit
