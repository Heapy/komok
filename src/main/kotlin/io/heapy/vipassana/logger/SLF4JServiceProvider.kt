package io.heapy.vipassana.logger

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import java.util.concurrent.atomic.AtomicInteger

class SLF4JServiceProvider : org.slf4j.spi.SLF4JServiceProvider {
    private var level: AtomicInteger = AtomicInteger(0)
    private val loggerFactory = ILoggerFactory(
        level = level::get,
        loggingSystem = LoggingSystem.instance,
    )
    private val markerFactory = BasicMarkerFactory()
    private val mdcAdapter = NOPMDCAdapter()

    override fun getLoggerFactory() = loggerFactory
    override fun getMarkerFactory() = markerFactory
    override fun getMDCAdapter() = mdcAdapter
    override fun getRequestedApiVersion() = "2.0"
    override fun initialize() {
    }
}

class ILoggerFactory(
    private val level: () -> Int,
    private val loggingSystem: LoggingSystem,
) : org.slf4j.ILoggerFactory {
    override fun getLogger(name: String?): Logger =
        Slf4jAdapter(
            level = level,
            vipassanaLogger = loggingSystem.getLogger(name ?: "ROOT")
        )
}

class Slf4jAdapter(
    private val level: () -> Int,
    private val vipassanaLogger: VipassanaLogger,
) : Logger {
    override fun getName(): String = vipassanaLogger.name

    override fun isTraceEnabled() =
        level() > 50

    override fun isTraceEnabled(marker: org.slf4j.Marker?) =
        level() > 50

    override fun trace(msg: String?) {
        runBlocking {
            vipassanaLogger.log(LoggingLevel.DEBUG, msg)
        }
    }

    override fun trace(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: org.slf4j.Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: org.slf4j.Marker?, format: String?, vararg argArray: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(marker: org.slf4j.Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: org.slf4j.Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(marker: org.slf4j.Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun info(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun info(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: org.slf4j.Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isWarnEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isWarnEnabled(marker: org.slf4j.Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun warn(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: org.slf4j.Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(marker: org.slf4j.Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(msg: String?) {
        TODO("Not yet implemented")
    }

    override fun error(format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: org.slf4j.Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: org.slf4j.Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: org.slf4j.Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }
}
