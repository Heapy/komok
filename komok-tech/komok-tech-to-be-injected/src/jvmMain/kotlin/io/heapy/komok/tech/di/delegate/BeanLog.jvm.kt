package io.heapy.komok.tech.di.delegate

import org.slf4j.LoggerFactory

private val beanLog = LoggerFactory.getLogger(MutableBean::class.java)

internal actual fun logBeanInfo(message: String) {
    beanLog.info(message)
}

internal actual fun logBeanError(
    message: String,
    throwable: Throwable,
) {
    beanLog.error(message, throwable)
}
