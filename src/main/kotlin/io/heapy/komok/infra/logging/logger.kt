package io.heapy.komok.infra.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> logger(): Logger = getLogger(T::class.java.name)

fun logger(func: () -> Unit): Logger {
    val className = func.javaClass.name
    val name = when {
        className.contains("Kt$") -> className.substringBefore("Kt$")
        className.contains("$") -> className.substringBefore("$")
        else -> className
    }
    return getLogger(name)
}

@Suppress("NOTHING_TO_INLINE")
inline fun getLogger(name: String): Logger {
    return LoggerFactory.getILoggerFactory().getLogger(name)
}
