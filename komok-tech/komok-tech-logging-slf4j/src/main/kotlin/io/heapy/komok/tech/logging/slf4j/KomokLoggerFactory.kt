package io.heapy.komok.tech.logging.slf4j

import io.heapy.komok.tech.logging.LoggingContext
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Logger factory that creates [KomokSLF4JLogger] instances.
 */
class KomokLoggerFactory(
    private val context: LoggingContext?,
) : ILoggerFactory {

    private val loggers = ConcurrentHashMap<String, Logger>()

    override fun getLogger(name: String): Logger =
        loggers.computeIfAbsent(name) { KomokSLF4JLogger(it, context) }
}
