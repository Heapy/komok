package io.heapy.komok.tech.logging.slf4j

import io.heapy.komok.tech.logging.Level
import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.logging.Mdc
import io.heapy.komok.tech.logging.ThrowableProxy
import org.slf4j.Marker
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter

/**
 * SLF4J Logger implementation that bridges to Komok Logging.
 *
 * Per-request MDC is NOT available for bridged third-party libs.
 */
class KomokSLF4JLogger(
    name: String,
    private val context: () -> LoggingContext?,
) : LegacyAbstractLogger() {

    init {
        this.name = name
    }

    override fun isTraceEnabled(): Boolean = isEnabled(Level.TRACE)
    override fun isDebugEnabled(): Boolean = isEnabled(Level.DEBUG)
    override fun isInfoEnabled(): Boolean = isEnabled(Level.INFO)
    override fun isWarnEnabled(): Boolean = isEnabled(Level.WARN)
    override fun isErrorEnabled(): Boolean = isEnabled(Level.ERROR)

    private fun isEnabled(level: Level): Boolean {
        val ctx = context() ?: return false
        return ctx.handler.isEnabled(level, name)
    }

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: org.slf4j.event.Level,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?,
    ) {
        val ctx = context() ?: return
        val komokLevel = mapLevel(level)
        if (!ctx.handler.isEnabled(komokLevel, name)) return

        val formattedMessage = if (arguments != null && messagePattern != null) {
            MessageFormatter.arrayFormat(messagePattern, arguments).message
        } else {
            messagePattern ?: ""
        }

        val event = LogEvent(
            level = komokLevel,
            loggerName = name,
            message = formattedMessage,
            throwable = throwable?.let { ThrowableProxy.create(it) },
            timestamp = ctx.now(),
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
