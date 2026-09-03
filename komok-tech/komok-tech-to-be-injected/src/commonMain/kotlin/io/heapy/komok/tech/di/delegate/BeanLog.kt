package io.heapy.komok.tech.di.delegate

internal expect fun logBeanInfo(message: String)

internal expect fun logBeanError(
    message: String,
    throwable: Throwable,
)
