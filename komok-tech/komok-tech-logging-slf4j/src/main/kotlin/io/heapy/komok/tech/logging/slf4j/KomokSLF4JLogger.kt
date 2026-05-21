package io.heapy.komok.tech.logging.slf4j

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.logging.Mdc
import io.heapy.komok.tech.logging.ThrowableProxy
import org.slf4j.Marker
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter
import kotlin.time.Clock

/**
 * SLF4J Logger implementation that bridges to Komok Logging.
 *
 * Uses the fixed [LoggingContext] provided at construction time.
 * Per-request MDC is NOT available for bridged third-party libs
 * (trade-off of no global state).
 */
class KomokSLF4JLogger(
    name: String,
    private val context: LoggingContext?,
) : LegacyAbstractLogger() {

    init {
        this.name = name
    }

    override fun isTraceEnabled(): Boolean = context != null
    override fun isDebugEnabled(): Boolean = context != null
    override fun isInfoEnabled(): Boolean = context != null
    override fun isWarnEnabled(): Boolean = context != null
    override fun isErrorEnabled(): Boolean = context != null

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: org.slf4j.event.Level,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?,
    ) {
        val ctx = context ?: return

        val formattedMessage = if (arguments != null && messagePattern != null) {
            MessageFormatter.arrayFormat(messagePattern, arguments).message
        } else {
            messagePattern ?: ""
        }

        val event = LogEvent(
            level = mapLevel(level),
            loggerName = name,
            message = formattedMessage,
            throwable = throwable?.let { ThrowableProxy.create(it) },
            timestamp = Clock.System.now(),
            mdc = Mdc.Empty,
        )

        ctx.handler.handle(event)
    }

    private fun mapLevel(level: org.slf4j.event.Level): Level = when (level) {
        org.slf4j.event.Level.TRACE -> Level.TRACE
        org.slf4j.event.Level.DEBUG -> Level.DEBUG
        org.slf4j.event.Level.INFO -> Level.INFO
        org.slf4j.event.Level.WARN -> Level.WARN
        org.slf4j.event.Level.ERROR -> Level.ERROR
    }
}
