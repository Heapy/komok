package io.heapy.komok.tech.logging

import java.nio.file.Path
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Build a [LoggingContext] using the configuration DSL.
 */
fun loggingContext(block: PipelineDsl.() -> Unit): LoggingContext {
    val pipeline = PipelineDsl().apply(block).build()
    return LoggingContext(handler = pipeline)
}

/**
 * DSL scope for building a handler pipeline.
 */
@LoggingDslMarker
class PipelineDsl {
    private val handlers = mutableListOf<LogHandler>()
    private val wrappers = mutableListOf<LogHandlerWrapper>()

    fun level(minLevel: Level) {
        wrappers += LevelFilter(minLevel)
    }

    fun filter(predicate: EventFilter) {
        wrappers += PredicateFilter(predicate)
    }

    fun loggerFilter(prefix: String) {
        wrappers += LoggerNameFilter(prefix)
    }

    fun sample(rate: Int) {
        wrappers += SamplingFilter(rate)
    }

    fun async(
        bufferSize: Int = 1024,
        overflow: OverflowPolicy = OverflowPolicy.DROP_ALL,
        highPriorityBlockTimeout: Duration = 50.milliseconds,
    ) {
        wrappers += AsyncHandler(
            bufferSize = bufferSize,
            overflowPolicy = overflow,
            highPriorityBlockTimeout = highPriorityBlockTimeout,
        )
    }

    fun <T : Any> enrich(key: Mdc.Key<T>, value: T) {
        wrappers += MdcEnricher(key, value)
    }

    fun wrap(wrapper: LogHandlerWrapper) {
        wrappers += wrapper
    }

    fun console(block: ConsoleAppenderDsl.() -> Unit = {}) {
        handlers += ConsoleAppenderDsl().apply(block).build()
    }

    fun file(path: String, block: FileAppenderDsl.() -> Unit = {}) {
        handlers += FileAppenderDsl(path).apply(block).build()
    }

    fun handler(handler: LogHandler) {
        handlers += handler
    }

    fun pipeline(block: PipelineDsl.() -> Unit) {
        handlers += PipelineDsl().apply(block).build()
    }

    fun route(block: RouteDsl.() -> Unit) {
        handlers += RouteDsl().apply(block).build()
    }

    fun <T : Any> routeByMdc(key: Mdc.Key<T>, block: MdcRouteDsl<T>.() -> Unit) {
        handlers += MdcRouteDsl(key).apply(block).build()
    }

    fun tee(block: TeeDsl.() -> Unit) {
        handlers += TeeDsl().apply(block).build()
    }

    fun build(): LogHandler {
        val composite = LogHandler.composite(handlers)
        return composite.wrappedBy(wrappers)
    }
}

@LoggingDslMarker
class ConsoleAppenderDsl {
    var target: ConsoleTarget = ConsoleTarget.STDOUT
    private var encoder: Encoder? = null

    fun encoder(encoder: Encoder) {
        this.encoder = encoder
    }

    fun format(block: FormatBuilder.() -> Unit) {
        this.encoder = io.heapy.komok.tech.logging.format(block)
    }

    fun json(prettyPrint: Boolean = false, staticFields: Map<String, String> = emptyMap()) {
        encoder = JsonEncoder(prettyPrint, staticFields)
    }

    fun build(): LogHandler = ConsoleAppender(
        encoder = encoder ?: defaultEncoder(),
        target = target,
    )
}

@LoggingDslMarker
class FileAppenderDsl(private val path: String) {
    private var encoder: Encoder? = null
    private var rolling: RollingPolicy? = null
    private var compression: Compression? = null

    fun encoder(encoder: Encoder) {
        this.encoder = encoder
    }

    fun format(block: FormatBuilder.() -> Unit) {
        this.encoder = io.heapy.komok.tech.logging.format(block)
    }

    fun json(prettyPrint: Boolean = false, staticFields: Map<String, String> = emptyMap()) {
        encoder = JsonEncoder(prettyPrint, staticFields)
    }

    fun rollBySize(maxBytes: Long, maxHistory: Int = 5) {
        rolling = SizeBasedRolling(maxBytes, maxHistory)
    }

    fun rollDaily(datePattern: String = "yyyy-MM-dd") {
        rolling = TimeBasedRolling(datePattern)
    }

    fun compressGzip() {
        compression = Compression.Gzip
    }

    fun build(): LogHandler = FileAppender(
        encoder = encoder ?: defaultEncoder(),
        path = Path.of(path),
        rolling = rolling,
        compression = compression,
    )
}

@LoggingDslMarker
class RouteDsl {
    private val routes = mutableListOf<Pair<String, LogHandler>>()
    private var fallback: LogHandler = LogHandler.noop()

    fun logger(prefix: String, block: PipelineDsl.() -> Unit) {
        routes += prefix to PipelineDsl().apply(block).build()
    }

    fun otherwise(block: PipelineDsl.() -> Unit) {
        fallback = PipelineDsl().apply(block).build()
    }

    fun build(): LogHandler = LoggerRouter(routes, fallback)
}

@LoggingDslMarker
class MdcRouteDsl<T : Any>(private val key: Mdc.Key<T>) {
    private val routes = mutableMapOf<T, LogHandler>()
    private var fallback: LogHandler = LogHandler.noop()

    fun value(value: T, block: PipelineDsl.() -> Unit) {
        routes[value] = PipelineDsl().apply(block).build()
    }

    fun otherwise(block: PipelineDsl.() -> Unit) {
        fallback = PipelineDsl().apply(block).build()
    }

    fun build(): LogHandler = MdcRouter(key, routes, fallback)
}

@LoggingDslMarker
class TeeDsl {
    private val branches = mutableListOf<Pair<EventFilter, LogHandler>>()

    fun branch(predicate: EventFilter, block: PipelineDsl.() -> Unit) {
        branches += predicate to PipelineDsl().apply(block).build()
    }

    fun all(block: PipelineDsl.() -> Unit) {
        branch({ true }, block)
    }

    fun build(): LogHandler = Tee(branches)
}

private fun defaultEncoder(): Encoder = format {
    timestamp()
    literal(" [")
    level()
    literal("] [")
    threadName()
    literal("] ")
    loggerShort()
    literal(" - ")
    message()
    throwableTrace()
}
