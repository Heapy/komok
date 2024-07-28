package io.heapy.komok.infra.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> logger(): Logger = getLogger(T::class.java.name)

@Suppress("NOTHING_TO_INLINE")
inline fun logger(noinline func: () -> Unit): Logger {
    return getLogger(func.javaClass.name)
}

@Suppress("NOTHING_TO_INLINE")
inline fun getLogger(className: String): Logger {
    val name = when {
        className.contains("Kt$") -> className.substringBefore("Kt$")
        className.contains("$") -> className.substringBefore("$")
        else -> className
    }

    return LoggerFactory
        .getILoggerFactory() // Skip one stack frame
        .getLogger(name)
}


open class Logger {
    protected val log: Logger = getLogger(this.javaClass.name)
}
