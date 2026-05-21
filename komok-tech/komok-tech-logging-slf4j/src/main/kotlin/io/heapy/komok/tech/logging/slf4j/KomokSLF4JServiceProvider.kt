package io.heapy.komok.tech.logging.slf4j

import io.heapy.komok.tech.logging.LoggingContext
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

/**
 * SLF4J ServiceProvider that bridges SLF4J logging calls to Komok Logging.
 *
 * **No global state.** Must be initialized with an explicit [LoggingContext]
 * before any logging occurs.
 *
 * Register via `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`.
 */
class KomokSLF4JServiceProvider : SLF4JServiceProvider {

    private lateinit var loggerFactory: KomokLoggerFactory
    private val markerFactory = BasicMarkerFactory()
    private val mdcAdapter = NOPMDCAdapter()

    override fun getLoggerFactory(): ILoggerFactory = loggerFactory
    override fun getMarkerFactory(): IMarkerFactory = markerFactory
    override fun getMDCAdapter(): MDCAdapter = mdcAdapter
    override fun getRequestedApiVersion(): String = "2.0.99"

    override fun initialize() {
        loggerFactory = KomokLoggerFactory(null)
    }

    fun initialize(context: LoggingContext) {
        loggerFactory = KomokLoggerFactory(context)
    }
}
