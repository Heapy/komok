package io.heapy.komok.tech.runtime.wrappers.sample

import io.heapy.komok.tech.logging.LogEvent
import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.runtime.fixtures.generatedKspMainSourceRoot
import io.heapy.komok.tech.runtime.wrappers.MethodMeta
import io.heapy.komok.tech.runtime.wrappers.ParameterMeta
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList

class WrapperGraphTest {
    /** #15 — @Log decorator logs entry and timed exit around the delegate, under the caller-provided context. */
    @Test
    fun `log decorator logs entry and timed exit around the delegate call`() = runTest {
        val events = CopyOnWriteArrayList<LogEvent>()
        val loggingContext = LoggingContext(handler = { events += it })
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val greeter = graph.modules.wrapperModule.greeter()

        val result = withWrapperContexts(loggingContext, registry) {
            greeter.greet("Ada")
        }

        val messages = events.map { it.message }
        assertAll(
            { assertEquals("Hello, Ada", result) },
            { assertTrue(messages.any { it == "Calling greet()" }, "entry log: $messages") },
            { assertTrue(messages.any { it.startsWith("greet returned: Hello, Ada") }, "exit log: $messages") },
            { assertTrue(events.all { it.loggerName.endsWith(".Greeter") }, "logger name") },
        )
        graph.stop()
    }

    /** #15 — @Metric decorator times the call against the caller-provided registry. */
    @Test
    fun `metric decorator records a timer in the caller-provided registry`() = runTest {
        val loggingContext = LoggingContext(handler = {})
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val greeter = graph.modules.wrapperModule.greeter()

        val greeting = withWrapperContexts(loggingContext, registry) {
            greeter.greet("Lin")
        }

        val timer = registry
            .find("io.heapy.komok.tech.runtime.wrappers.sample.Greeter.greet")
            .tag("outcome", "success")
            .timer()
        assertAll(
            { assertEquals("Hello, Lin", greeting) },
            { assertEquals(1L, timer?.count()) },
        )
        graph.stop()
        registry.close()
    }

    /** #15 — @Retry decorator re-invokes the delegate until it succeeds. */
    @Test
    fun `retry decorator re-invokes the delegate until it succeeds`() = runTest {
        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val flaky = graph.modules.wrapperModule.flakyService()

        // RealFlaky fails on attempts 1 and 2, succeeds on attempt 3.
        val result = flaky.attempt()

        assertEquals(3, result)
        graph.stop()
    }

    /** #15 — generated chain order is Log -> Metric -> Retry -> delegate. */
    @Test
    fun `combined decorators wrap once outside retry and retry only the delegate`() = runTest {
        val events = CopyOnWriteArrayList<LogEvent>()
        val loggingContext = LoggingContext(handler = { events += it })
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val chained = graph.modules.wrapperModule.chainedService()

        val result = withWrapperContexts(loggingContext, registry) {
            chained.operation("Ivy")
        }
        val timer = registry
            .find("io.heapy.komok.tech.runtime.wrappers.sample.ChainedService.operation")
            .tag("outcome", "success")
            .timer()
        val messages = events.map { it.message }

        assertAll(
            { assertEquals("ok:Ivy:3", result) },
            { assertEquals(3, chained.attempts()) },
            { assertEquals(1, messages.count { it == "Calling operation()" }, "entry log: $messages") },
            { assertEquals(1, messages.count { it.startsWith("operation returned: ok:Ivy:3") }, "exit log: $messages") },
            { assertEquals(1L, timer?.count()) },
        )
        graph.stop()
        registry.close()
    }

    /** #15 — method identity is generated static metadata, not runtime reflection. */
    @Test
    fun `generated metadata describes the decorated method statically`() {
        assertEquals(
            MethodMeta(
                owner = "io.heapy.komok.tech.runtime.wrappers.sample.ChainedService",
                name = "operation",
                parameters = listOf(ParameterMeta("name", "kotlin.String")),
                returnType = "kotlin.String",
                isSuspend = true,
            ),
            ChainedServiceMeta.operation,
        )
    }

    /** #15 — generated runtime code does not use kotlin.reflect or bytecode proxy APIs. */
    @Test
    fun `generated decorator sources avoid reflection and proxy dependencies`() {
        val generatedRoot = generatedKspMainSourceRoot()
        val bannedTokens = listOf("kotlin.reflect", "KCallable", "KFunction", "KClass", "ByteBuddy")
        val hits = Files.walk(generatedRoot).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) }
                .filter { path -> path.fileName.toString().endsWith(".kt") }
                .flatMap { path ->
                    val text = Files.readString(path)
                    bannedTokens
                        .filter { token -> text.contains(token) }
                        .map { token -> "${generatedRoot.relativize(path)} contains $token" }
                        .stream()
                }
                .toList()
        }

        assertEquals(emptyList<String>(), hits)
    }

    /** replaceXWith {} swaps the raw provider body but keeps production decoration. */
    @Test
    fun `replaceWith body replacement keeps the production decorators`() = runTest {
        val events = CopyOnWriteArrayList<LogEvent>()
        val loggingContext = LoggingContext(handler = { events += it })
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val fork = graph.fork {
            wrapperModule { replaceGreeterWith { RealGreeter() } }
        }
        val greeter = fork.modules.wrapperModule.greeter()

        val result = withWrapperContexts(loggingContext, registry) {
            greeter.greet("Eva")
        }

        val messages = events.map { it.message }
        assertAll(
            { assertEquals("Hello, Eva", result) },
            // the replacement value went through Greeter_KomokDecorated again.
            { assertTrue(messages.any { it == "Calling greet()" }, "entry log: $messages") },
        )
        fork.stop()
        graph.stop()
    }

    /** replaceX(value) is the terminal override: the pinned value is NOT decorated. */
    @Test
    fun `replace value pins the instance and strips the production decorators`() = runTest {
        val events = CopyOnWriteArrayList<LogEvent>()
        val loggingContext = LoggingContext(handler = { events += it })
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val pinned = RealGreeter()
        val fork = graph.fork {
            wrapperModule { replaceGreeter(pinned) }
        }
        val greeter = fork.modules.wrapperModule.greeter()

        val result = withWrapperContexts(loggingContext, registry) {
            greeter.greet("Mia")
        }

        assertAll(
            { assertEquals("Hello, Mia", result) },
            { assertSame(pinned, greeter) },
            { assertTrue(events.isEmpty(), "no decoration expected: ${events.map { it.message }}") },
        )
        fork.stop()
        graph.stop()
    }

    /** Generated mocks can be terminal fork replacements with stubbing and verification. */
    @Test
    fun `generated mock can stub verify and replace a service in a fork`() = runTest {
        val events = CopyOnWriteArrayList<LogEvent>()
        val loggingContext = LoggingContext(handler = { events += it })
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val mock = greeterMock()
        mock.everyGreet { name -> "mock:$name" }
        val fork = graph.fork {
            wrapperModule { replaceGreeter(mock) }
        }
        val greeter = fork.modules.wrapperModule.greeter()

        val result = withWrapperContexts(loggingContext, registry) {
            greeter.greet("Nia")
        }

        assertAll(
            { assertEquals("mock:Nia", result) },
            { mock.verifyGreet("Nia") },
            { assertSame(mock, greeter) },
            { assertTrue(events.isEmpty(), "terminal mock replacement should not be decorated") },
        )
        fork.stop()
        graph.stop()
        registry.close()
    }

    /** Generated spies delegate by default while still allowing targeted stubs and verification. */
    @Test
    fun `generated spy delegates unstubbed calls and verifies stubbed calls`() = runTest {
        val events = CopyOnWriteArrayList<LogEvent>()
        val loggingContext = LoggingContext(handler = { events += it })
        val registry = SimpleMeterRegistry()

        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val spy = greeterSpy(RealGreeter())
        spy.everyFarewell { name -> "ciao:$name" }
        val fork = graph.fork {
            wrapperModule { replaceGreeter(spy) }
        }
        val greeter = fork.modules.wrapperModule.greeter()

        val greeting = withWrapperContexts(loggingContext, registry) {
            greeter.greet("Ira")
        }
        val farewell = greeter.farewell("Ira")

        assertAll(
            { assertEquals("Hello, Ira", greeting) },
            { assertEquals("ciao:Ira", farewell) },
            { spy.verifyGreet("Ira") },
            { spy.verifyFarewell("Ira") },
            { assertSame(spy, greeter) },
            { assertTrue(events.isEmpty(), "terminal spy replacement should not be decorated") },
        )
        fork.stop()
        graph.stop()
        registry.close()
    }

    /** An un-annotated method on a decoratable interface is overridden but plain-delegated. */
    @Test
    fun `unwrapped method is plain delegated`() = runTest {
        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val greeter = graph.modules.wrapperModule.greeter()

        assertEquals("Bye, Bob", greeter.farewell("Bob"))
        graph.stop()
    }

    /** A default interface method is delegated too: the implementation's override wins. */
    @Test
    fun `default interface method is delegated to the implementation override`() = runTest {
        val graph = createWrapperModuleGraph { dispatcher = Dispatchers.Default }
        val greeter = graph.modules.wrapperModule.greeter()

        // the interface default would produce "BYE, ZED!"; RealGreeter's override differs.
        assertEquals("HELLO, Zed!", greeter.shout("Zed"))
        graph.stop()
    }
}

/** Supplies the wrapper context parameters positionally — the mechanism the runtime itself uses. */
private suspend fun <T> withWrapperContexts(
    loggingContext: LoggingContext,
    registry: MeterRegistry,
    block: suspend context(LoggingContext, MeterRegistry) () -> T,
): T = block(loggingContext, registry)
