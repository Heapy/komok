package io.heapy.vipassana.logger

import java.lang.Exception
import java.time.LocalDateTime

class Logger(
    private val name: String,
) {
    fun info(
        message: String,
    ) {
        println(buildString {
            append(LocalDateTime.now().toString())
            append("|I|")
            append(Thread.currentThread().name)
            append("|")
            append(name)
            append("|")
            append(message)
        })
    }

    fun error(
        message: String,
        exception: Exception? = null
    ) {
        System.err.println(buildString {
            append(LocalDateTime.now().toString())
            append("|E|")
            append(Thread.currentThread().name)
            append("|")
            append(name)
            append("|")
            append(message)
            exception?.let {
                appendLine()
                append(exception.stackTraceToString())
            }
        })
    }

    companion object {
        inline operator fun <reified T : Any> invoke(): Logger =
            Logger(T::class.qualifiedName ?: error("No qualified name for ${T::class}"))
    }
}
