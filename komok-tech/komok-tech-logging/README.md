# Komok Logging

Modern Kotlin logging library built on context parameters, type-safe MDC, and composable handler pipelines (Undertow-inspired).

**Requires Kotlin 2.1+** (context parameters)

## Design Principles

- **No global state.** `LoggingContext` flows through Kotlin context parameters — no static `LoggerFactory`, no `ThreadLocal` MDC. The SLF4J bridge is the one exception, because `ServiceLoader` creates it.
- **Logger is just a name.** Zero-cost `@JvmInline value class`, delegates all work to the `LoggingContext`.
- **MDC is type-safe and immutable.** Inspired by `CoroutineContext.Key<E>`.
- **Configuration is a handler pipeline.** Filters, routers, appenders — composed via `LogHandler` / `LogHandlerWrapper`.
- **No pattern string parsing.** Format built programmatically via `FormatElement` sealed class and builder DSL.

## Module Structure

| Module | Purpose | Platform |
|--------|---------|----------|
| `komok-tech-logging-api` | Public API: Logger, LogEvent, Level, Mdc, LoggingContext, LogHandler, Encoder, FormatElement, ThrowableProxy | JVM |
| `komok-tech-logging` | Implementation: appenders, encoders, DSL, async, routing, filters, enrichment, config manager | JVM |
| `komok-tech-logging-slf4j` | SLF4J ServiceProvider bridge | JVM |
| `komok-tech-logging-jul` | java.util.logging Handler bridge | JVM |

Package: `io.heapy.komok.tech.logging`

## Quick Start

```kotlin
// 1. Configure
val ctx = loggingContext {
    level(Level.INFO)
    console {
        format {
            timestamp()
            literal(" [")
            level()
            literal("] ")
            loggerShort()
            literal(" - ")
            message()
            throwableTrace()
        }
    }
}

// 2. Use — LoggingContext flows via context parameters
class OrderService {
    private val log = logger<OrderService>()

    context(_: LoggingContext)
    fun placeOrder(order: Order) {
        log.info { "Placing order ${order.id}" }
    }
}

// 3. Run — derive MDC per-request, pass automatically
with(ctx.withMdc(RequestId, "req-123")) {
    orderService.placeOrder(order)
}
```

## Architecture

```
Application Code
    | context(LoggingContext)
    v
LoggingContext -- handler: LogHandler
    |               |
    |               v
    |   +- LevelFilter ------ wrapper
    |   +- LoggerNameFilter -- wrapper
    |   +- PredicateFilter --- wrapper
    |   +- SamplingFilter ---- wrapper
    |   +- AsyncHandler ------ wrapper
    |   +- MdcEnricher ------- wrapper
    |   +- LoggerRouter ------ handler (sifting)
    |   +- MdcRouter --------- handler (sifting)
    |   +- Tee --------------- handler (fan-out)
    |               |
    |               v
    |         Appenders (Console, File)
    |               |
    |               v
    |         Encoders (FormatEncoder, JsonEncoder)
    |
    +-- mdc: Mdc -- type-safe key-value context
```

## API (komok-tech-logging-api)

### Level

```kotlin
enum class Level { TRACE, DEBUG, INFO, WARN, ERROR, OFF }
```

### Logger

`@JvmInline value class`. All logging methods require `context(LoggingContext)`. Each call asks the pipeline whether the level is enabled first, so a rejected call never runs the message lambda and never builds an event.

```kotlin
@JvmInline
value class Logger(val name: String) {
    context(ctx: LoggingContext)
    inline fun info(throwable: Throwable? = null, msg: () -> String)
    // trace, debug, warn, error — same signature

    context(ctx: LoggingContext)
    fun isEnabled(level: Level): Boolean
}

inline fun <reified T> logger(): Logger
fun logger(name: String): Logger
fun Any.logger(): Logger
```

### LogEvent

Regular class (NOT data class). Lazy `threadName` avoids unnecessary computation.

```kotlin
class LogEvent(
    val level: Level,
    val loggerName: String,
    val message: String,
    val throwable: ThrowableProxy?,
    val timestamp: Instant,
    val mdc: Mdc,
) {
    val threadName: String by lazy { Thread.currentThread().name }
    fun prepareForDeferredProcessing()  // forces lazy fields before async handoff
}
```

### ThrowableProxy

Immutable snapshot with cycle detection via `IdentityHashMap` and common-frame elision.

### Mdc

Sealed interface. Immutable, keyed by typed objects.

```kotlin
sealed interface Mdc {
    abstract class Key<T : Any>(val name: String)

    operator fun <T : Any> get(key: Key<T>): T?
    fun <T : Any> with(key: Key<T>, value: T): Mdc
    operator fun plus(other: Mdc): Mdc
    fun <T : Any> minus(key: Key<T>): Mdc
    fun entries(): Map<String, Any>
    val size: Int
    val isEmpty: Boolean

    companion object {
        val Empty: Mdc
        fun <T : Any> of(key: Key<T>, value: T): Mdc
    }
}

// Usage
object RequestId : Mdc.Key<String>("requestId")
val mdc = Mdc.of(RequestId, "req-abc-123")
val rid: String? = mdc[RequestId]  // typed, no cast
```

Builder DSL:

```kotlin
val mdc = buildMdc {
    RequestId("req-abc-123")
    UserId to 42L
}
```

### LoggingContext

Immutable context flowing via Kotlin context parameters.

```kotlin
class LoggingContext(
    val handler: LogHandler,
    val mdc: Mdc = Mdc.Empty,
    val now: () -> Instant = { Clock.System.now() },
) {
    fun withMdc(mdc: Mdc): LoggingContext
    fun <T : Any> withMdc(key: Mdc.Key<T>, value: T): LoggingContext
    inline fun withMdc(block: MdcBuilder.() -> Unit): LoggingContext
}
```

Scoped helpers:

```kotlin
context(outer: LoggingContext)
inline fun <T> withMdc(mdc: Mdc, block: context(LoggingContext) () -> T): T

context(outer: LoggingContext)
inline fun <R : Any, T> withMdc(key: Mdc.Key<R>, value: R, block: context(LoggingContext) () -> T): T
```

### LogHandler / LogHandlerWrapper

Undertow-inspired pipeline composition.

`isEnabled` is the fast path: it answers whether an event of that level and logger name can reach any appender, without an event to inspect. Implementations must stay side-effect free and answer `true` whenever the decision needs the event, so `LevelFilter` and `LoggerNameFilter` give real answers while `PredicateFilter`, `SamplingFilter` and `Tee` pass the question down.

```kotlin
fun interface LogHandler {
    fun handle(event: LogEvent)
    fun isEnabled(level: Level, loggerName: String): Boolean = true
    companion object {
        fun noop(): LogHandler
        fun composite(vararg handlers: LogHandler): LogHandler
    }
}

fun interface LogHandlerWrapper {
    fun wrap(next: LogHandler): LogHandler
    companion object { fun identity(): LogHandlerWrapper }
}

infix fun LogHandlerWrapper.andThen(inner: LogHandlerWrapper): LogHandlerWrapper
fun LogHandler.wrappedBy(vararg wrappers: LogHandlerWrapper): LogHandler
```

### Encoder / FormatElement

```kotlin
fun interface Encoder {
    fun encode(event: LogEvent): String
}

sealed class FormatElement {
    data object Timestamp : FormatElement()
    data object Message : FormatElement()
    data object Level : FormatElement()
    data object LoggerName : FormatElement()
    data class LoggerNameShort(val segments: Int = 1) : FormatElement()
    data object ThreadName : FormatElement()
    data object AllMdc : FormatElement()
    data class MdcValue(val key: String) : FormatElement()
    data class Literal(val text: String) : FormatElement()
    data object Newline : FormatElement()
    data object ThrowableTrace : FormatElement()
}

typealias EventFilter = (LogEvent) -> Boolean
```

## Implementation (komok-tech-logging)

### Filters (LogHandlerWrappers)

| Filter | Description |
|--------|-------------|
| `LevelFilter(minLevel)` | Threshold filter |
| `LoggerNameFilter(prefix, matchExact)` | Logger name prefix match |
| `PredicateFilter(predicate)` | Generic `EventFilter` predicate |
| `SamplingFilter(rate)` | Every Nth event |

### Routers (LogHandlers)

| Router | Description |
|--------|-------------|
| `Router<K>(discriminator, routes, fallback)` | Generic key-based routing |
| `LoggerRouter(routes, fallback)` | Logger name prefix routing, first match wins |
| `MdcRouter<T>(key, routes, fallback)` | MDC value routing |
| `Tee(branches)` | Fan-out with per-branch predicates |

### Enrichment

| Wrapper | Description |
|---------|-------------|
| `MdcEnricher<T>(key, value)` | Adds fixed MDC entry to every event |

### Appenders

| Appender | Description |
|----------|-------------|
| `ConsoleAppender(encoder, target)` | STDOUT or STDERR |
| `FileAppender(encoder, path, rolling, compression)` | File with optional rolling and gzip compression |

Rolling policies: `SizeBasedRolling(maxBytes, maxHistory)`, `TimeBasedRolling(datePattern)`.

### Encoders

| Encoder | Description |
|---------|-------------|
| `FormatEncoder(elements)` | Renders via `FormatElement` list — no pattern string parsing |
| `JsonEncoder(prettyPrint, staticFields)` | Structured JSON output, MDC as top-level fields |

### Async Handler

`AsyncHandler` — coroutine Channel-based wrapper with configurable overflow policy.

```kotlin
class AsyncHandler(
    bufferSize: Int = 1024,
    overflowPolicy: OverflowPolicy = OverflowPolicy.DROP_ALL,
    highPriorityBlockTimeout: Duration = 50.milliseconds,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LogHandlerWrapper, AutoCloseable
```

`OverflowPolicy`: `DROP_ALL`, `BLOCK_ALL`, `HYBRID` (drop TRACE/DEBUG/INFO; block WARN/ERROR up to timeout).

Observable counters: `enqueued`, `processed`, `dropped`, `failed`.

### Config Manager

Atomic hot-reload via `AtomicReference`:

```kotlin
class ConfigManager(initialContext: LoggingContext) {
    val current: LoggingContext
    fun reload(newContext: LoggingContext)
}
```

### DSL

```kotlin
val ctx = loggingContext {
    level(Level.INFO)
    filter { it.message.contains("important") }
    loggerFilter("com.myapp")
    sample(100)  // every 100th event
    async(bufferSize = 2048, overflow = OverflowPolicy.HYBRID)
    enrich(HostKey, "server-01")

    console {
        format { timestamp(); literal(" "); level(); literal(" "); message(); throwableTrace() }
    }

    file("/var/log/app.log") {
        json(prettyPrint = false, staticFields = mapOf("service" to "myapp"))
        rollDaily()
        compressGzip()
    }

    route {
        logger("com.myapp.audit") {
            file("/var/log/audit.log") { json() }
        }
        otherwise { console() }
    }

    routeByMdc(TenantId) {
        value("acme") { file("/var/log/acme.log") { json() } }
        otherwise { console() }
    }

    tee {
        branch({ it.level.ordinal >= Level.ERROR.ordinal }) { file("/var/log/errors.log") }
        all { console() }
    }

    pipeline {  // scoped sub-pipeline with own wrappers
        level(Level.DEBUG)
        console()
    }
}
```

All DSL classes annotated with `@LoggingDslMarker` to prevent scope leaking.

## Bridges

### SLF4J Bridge (komok-tech-logging-slf4j)

`isTraceEnabled` and friends report the pipeline's answer, and a rejected call is dropped before the message is formatted.

`KomokSLF4JServiceProvider` implements `SLF4JServiceProvider`. SLF4J creates it through `ServiceLoader`, so the `LoggingContext` is installed through `KomokSLF4JServiceProvider.loggingContext` and read on every call. Per-request MDC is not available for bridged third-party libs.

### JUL Bridge (komok-tech-logging-jul)

`KomokJulHandler` extends `java.util.logging.Handler`. Receives a fixed `LoggingContext` at construction time.

Level mapping: SEVERE->ERROR, WARNING->WARN, INFO->INFO, FINE->DEBUG, FINEST->TRACE.

## Context Flow — No Global State

1. **Startup**: Build `LoggingContext` with handler pipeline via DSL
2. **Per-request/scope**: Derive new context via `withMdc()` with scope-specific values
3. **Propagation**: Context flows via Kotlin context parameters through call chain
4. **In logger**: Event captures current MDC snapshot at creation time
5. **Handler pipeline**: Events flow through wrappers/filters to terminal appenders
6. **Bridges**: the JUL bridge receives a fixed `LoggingContext` at construction; the SLF4J bridge reads `KomokSLF4JServiceProvider.loggingContext`
