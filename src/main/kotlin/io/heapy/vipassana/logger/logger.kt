package io.heapy.vipassana.logger

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

interface LoggingContext {
    val context: Map<String, Any?>
}

enum class LoggingLevel(val level: Byte) {
    TRACE(1),
    DEBUG(2),
    INFO(3),
    WARN(4),
    ERROR(5),
    OFF(6),
}

data class LoggingConfiguration(
    val level: LoggingLevel = LoggingLevel.INFO,
    val pattern: LoggingPattern = DefaultLoggingPattern,
)

private object DefaultLoggingPattern : LoggingPattern {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd")
    override fun format(
        output: PrintStream,
        loggerName: String,
        level: LoggingLevel,
        vararg message: Any?,
        throwable: Throwable?,
        context: Map<String, Any?>?,
        thread: Thread,
        coroutineContext: CoroutineContext,
        instant: Instant,
    ) {
        output.print(formatter.format(instant.atZone(ZoneId.systemDefault())))
        output.print(" [")
        output.print(thread.name)
        coroutineContext[CoroutineName]?.let {
            output.print("|")
            output.print(it.name)
        }
        output.print("] ")
        output.print(level.name)
        output.print(" ")
        output.print(loggerName)
        output.print(" - ")
        message.forEach { output.print(it) }
        output.println()
    }
}

interface LoggingPattern {
    fun format(
        output: PrintStream,
        loggerName: String,
        level: LoggingLevel,
        vararg message: Any?,
        throwable: Throwable?,
        context: Map<String, Any?>?,
        thread: Thread,
        coroutineContext: CoroutineContext,
        instant: Instant,
    )
}

data class LoggingSystem(
    private val loggingConfiguration: LoggingConfiguration
) {
    fun getLogger(name: String): VipassanaLogger {
        return VipassanaLogger(
            name = name,
            effectiveLevel = loggingConfiguration.level,
            appender = ActorConsoleAppender(loggingConfiguration.pattern),
        )
    }

    companion object {
        val instance = LoggingSystem(
            loggingConfiguration = LoggingConfiguration(),
        )
    }
}

class VipassanaLogger(
    val name: String,
    private val effectiveLevel: LoggingLevel,
    private val appender: Appender,
) {
    context(LoggingContext)
        suspend fun log(level: LoggingLevel, vararg message: Any?, throwable: Throwable? = null) {
        if (effectiveLevel.level <= level.level) {
            appender.log(
                LoggingEvent(
                    loggerName = name,
                    level = level,
                    message = message,
                    throwable = throwable,
                    context = context,
                    thread = Thread.currentThread(),
                    coroutineContext = coroutineContext,
                    instant = Instant.now(),
                )
            )
        }
    }

    suspend fun log(level: LoggingLevel, vararg message: Any?, throwable: Throwable? = null) {
        if (effectiveLevel.level <= level.level) {
            appender.log(
                LoggingEvent(
                    loggerName = name,
                    level = level,
                    message = message,
                    throwable = throwable,
                    context = null,
                    thread = Thread.currentThread(),
                    coroutineContext = coroutineContext,
                    instant = Instant.now(),
                )
            )
        }
    }
}

suspend fun main() = withContext(CoroutineName("Hmmm")) {

    LoggerFactory.getLogger("test").info("gello")

    val log = LoggingSystem(
        LoggingConfiguration(
            level = LoggingLevel.TRACE,
        ),
    )
        .getLogger("io.heapy.vipassana.logger.main")

    log.log(LoggingLevel.ERROR, "Hello E")
    log.log(LoggingLevel.INFO, "Hello I")
    log.log(LoggingLevel.WARN, "Hello W")
    log.log(LoggingLevel.DEBUG, "Hello D")
    log.log(LoggingLevel.TRACE, "Hello T")
    log.log(LoggingLevel.OFF, "Hello O")

    while (true) {
        log.log(LoggingLevel.ERROR, "Hello E")
    }
}

interface Appender {
    suspend fun log(
        loggingEvent: LoggingEvent,
    )
}

class LoggingEvent(
    val loggerName: String,
    val level: LoggingLevel,
    val message: Array<out Any?>,
    val throwable: Throwable?,
    val context: Map<String, Any?>?,
    val thread: Thread,
    val coroutineContext: CoroutineContext,
    val instant: Instant,
)

class ActorConsoleAppender(
    private val pattern: LoggingPattern,
) : Appender {
    private val actor = GlobalScope.actor<LoggingEvent>(
        Dispatchers.Default.limitedParallelism(1),
        capacity = 256
    ) {
        for (loggingEvent in channel) {
            pattern.format(
                output = System.out,
                loggerName = loggingEvent.loggerName,
                level = loggingEvent.level,
                message = loggingEvent.message,
                throwable = loggingEvent.throwable,
                context = loggingEvent.context,
                thread = loggingEvent.thread,
                coroutineContext = loggingEvent.coroutineContext,
                instant = loggingEvent.instant,
            )
        }
    }

    override suspend fun log(loggingEvent: LoggingEvent) {
        actor.send(loggingEvent)
    }
}

class ConsoleAppender(
    private val pattern: LoggingPattern,
) : Appender {
    private val mutex = Mutex()

    override suspend fun log(
        loggingEvent: LoggingEvent,
    ) {
        mutex.withLock {
            pattern.format(
                output = System.out,
                loggerName = loggingEvent.loggerName,
                level = loggingEvent.level,
                message = loggingEvent.message,
                throwable = loggingEvent.throwable,
                context = loggingEvent.context,
                thread = loggingEvent.thread,
                coroutineContext = loggingEvent.coroutineContext,
                instant = loggingEvent.instant,
            )
        }
    }
}

/**
 * Function to retrieve logger by class.
 *
 * @author Ruslan Ibragimov
 * @since 1.0
 */
inline fun <reified T : Any> LoggingSystem.logger(): VipassanaLogger =
    logger(T::class)

/**
 * Function to retrieve logger by class.
 *
 * @author Ruslan Ibragimov
 * @since 1.0
 */
fun <T : Any> LoggingSystem.logger(
    clazz: KClass<T>,
): VipassanaLogger =
    getLogger(clazz.qualifiedName ?: error("Class name is null"))

/**
 * Function to retrieve logger by name.
 *
 * @author Ruslan Ibragimov
 * @since 1.0
 */
fun LoggingSystem.logger(
    name: String,
): VipassanaLogger =
    getLogger(name)

/**
 * Function to retrieve logger by context where it's defined.
 * Can be used to get logger:
 *
 * ```
 * // in foo/bar/Service.kt
 * var logger = logger {}
 * fun main() {
 *     logger.info("Execute order 66")
 *     // 00:00:00.000 [main] INFO foo.bar.Service - Execute order 66
 * }
 * ```
 *
 * Can be used to log in place:
 *
 * ```
 * // in foo/bar/Service.kt
 * fun main() {
 *     logger {
 *         info { "Execute order 66" }
 *         // 00:00:00.000 [main] INFO foo.bar.Service - Execute order 66
 *     }
 * }
 * ```
 *
 * @author Ruslan Ibragimov
 * @since 1.0
 */
fun LoggingSystem.logger(
    context: VipassanaLogger.() -> Unit,
): VipassanaLogger {
    val name = context.javaClass.name
    val loggerName = when {
        name.contains("Kt$") -> name.substringBefore("Kt$")
        name.contains("$") -> name.substringBefore("$")
        else -> name
    }
    return getLogger(loggerName).apply(context)
}

/* EXTRA */

fun <T> T.debugState(
    logger: VipassanaLogger,
): T {
    runBlocking {
        logger.log(LoggingLevel.DEBUG, this.toString())
    }
    return this
}

