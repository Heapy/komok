package io.heapy.komok

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)
fun logger(func: () -> Unit): Logger = getLogger(func.javaClass.name)

private fun getLogger(className: String): Logger {
    val name = when {
        className.contains("Kt$") -> className.substringBefore("Kt$")
        className.contains("$") -> className.substringBefore("$")
        else -> className
    }
    return LoggerFactory.getLogger(name)
}
