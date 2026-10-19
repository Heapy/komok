package io.heapy.komok.tech.runtime.wrappers.sample

import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.runtime.wrappers.Log
import io.heapy.komok.tech.runtime.wrappers.Metric
import io.heapy.komok.tech.runtime.wrappers.Retry
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicInteger

/**
 * P7 sample: decoratable service interfaces returned by a `@Module`. The compiler plugin
 * generates interface decorators and wraps the
 * `greeter()` / `flakyService()` bindings, so reads return the decorated value.
 */

interface Greeter {
    // Wrapper context is caller-provided via context parameters, in canonical order
    // (@Log -> LoggingContext, @Metric -> MeterRegistry) — never static.
    @Log
    @Metric
    context(_: LoggingContext, _: MeterRegistry)
    suspend fun greet(name: String): String

    // Un-annotated: the decorator overrides it but plain-delegates.
    suspend fun farewell(name: String): String

    // Default implementation: the decorator must still delegate so an implementation's
    // override is honored — inheriting this body would silently bypass RealGreeter.
    suspend fun shout(name: String): String =
        farewell(name).uppercase()
}

class RealGreeter : Greeter {
    context(_: LoggingContext, _: MeterRegistry)
    override suspend fun greet(name: String): String = "Hello, $name"

    override suspend fun farewell(name: String): String = "Bye, $name"
    override suspend fun shout(name: String): String = "HELLO, $name!"
}

interface FlakyService {
    @Retry(maxAttempts = 5)
    suspend fun attempt(): Int
}

class RealFlaky(
    private val failuresBeforeSuccess: Int,
) : FlakyService {
    private val calls = AtomicInteger(0)

    override suspend fun attempt(): Int {
        val n = calls.incrementAndGet()
        if (n <= failuresBeforeSuccess) {
            error("flaky attempt $n")
        }
        return n
    }
}

interface ChainedService {
    @Log
    @Metric
    @Retry(maxAttempts = 4)
    context(_: LoggingContext, _: MeterRegistry)
    suspend fun operation(name: String): String

    fun attempts(): Int
}

class RealChainedService(
    private val failuresBeforeSuccess: Int,
) : ChainedService {
    private val calls = AtomicInteger(0)

    context(_: LoggingContext, _: MeterRegistry)
    override suspend fun operation(name: String): String {
        val attempt = calls.incrementAndGet()
        if (attempt <= failuresBeforeSuccess) {
            error("chain attempt $attempt")
        }
        return "ok:$name:$attempt"
    }

    override fun attempts(): Int =
        calls.get()
}

@Module
class WrapperModule {
    context(_: KomokContext)
    suspend fun greeter(): Greeter =
        RealGreeter()

    context(_: KomokContext)
    suspend fun flakyService(): FlakyService =
        RealFlaky(failuresBeforeSuccess = 2)

    context(_: KomokContext)
    suspend fun chainedService(): ChainedService =
        RealChainedService(failuresBeforeSuccess = 2)
}
