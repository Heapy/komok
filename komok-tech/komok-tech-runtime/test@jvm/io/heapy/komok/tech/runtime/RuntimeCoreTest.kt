package io.heapy.komok.tech.runtime

import io.heapy.komok.tech.runtime.fixtures.BranchyModuleKeys
import io.heapy.komok.tech.runtime.fixtures.BranchyModule_Komok
import io.heapy.komok.tech.runtime.fixtures.CycleModuleKeys
import io.heapy.komok.tech.runtime.fixtures.CycleModule_Komok
import io.heapy.komok.tech.runtime.fixtures.ServiceFailure
import io.heapy.komok.tech.runtime.fixtures.ServiceModule_Komok
import io.heapy.komok.tech.runtime.fixtures.UtilsModuleKeys
import io.heapy.komok.tech.runtime.fixtures.UtilsModule_Komok
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull

class RuntimeCoreTest {
    /** #1 — provider invoked once; repeated read returns the same instance. */
    @Test
    fun `memoization - provider runs once and read returns same instance`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = UtilsModule_Komok(rt)
        val handle = rt.rootHandle()

        val first = handle.read { module.clock() }
        val second = handle.read { module.clock() }

        assertAll(
            { assertSame(first, second) },
            { assertEquals(1, module.clockCalls.get()) },
            { assertEquals(1, module.timeSourceCalls.get()) },
        )
        rt.close()
    }

    /** #2 — realization records the intra-module edge clock -> timeSource. */
    @Test
    fun `edge recording - executed providers record edges`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = UtilsModule_Komok(rt)
        val handle = rt.rootHandle()

        val clock = handle.read { module.clock() }

        assertAll(
            { assertNotNull(clock) },
            {
                assertEquals(
                    setOf(Edge(UtilsModuleKeys.clock, UtilsModuleKeys.timeSource)),
                    rt.edges(),
                )
            },
        )
        rt.close()
    }

    /** #3 — only the taken branch records an edge. */
    @Test
    fun `branchy provider - only the executed branch records an edge`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = BranchyModule_Komok(rt, useConfig = true)
        val handle = rt.rootHandle()

        val value = handle.read { module.source() }

        assertAll(
            { assertEquals("config-source", value) },
            {
                assertEquals(
                    setOf(Edge(BranchyModuleKeys.source, BranchyModuleKeys.config)),
                    rt.edges(),
                )
            },
        )
        rt.close()
    }

    /** #3 — the alternate branch records only its own edge. */
    @Test
    fun `branchy provider - alternate branch records only the system edge`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = BranchyModule_Komok(rt, useConfig = false)
        val handle = rt.rootHandle()

        val value = handle.read { module.source() }

        assertAll(
            { assertEquals("system-source", value) },
            {
                assertEquals(
                    setOf(Edge(BranchyModuleKeys.source, BranchyModuleKeys.systemSource)),
                    rt.edges(),
                )
            },
        )
        rt.close()
    }

    /** #11 — a binding cycle throws KomokCycleException carrying the path. */
    @Test
    fun `binding cycle - throws KomokCycleException with the offending path`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = CycleModule_Komok(rt)
        val handle = rt.rootHandle()

        val failure = captureFailure { handle.read { module.a() } }

        val cycle = assertInstanceOf<KomokCycleException>(failure)
        assertEquals(
            listOf(CycleModuleKeys.a, CycleModuleKeys.b, CycleModuleKeys.a),
            cycle.path,
        )
        rt.close()
    }

    /** #12 — concurrent readers trigger one computation; the rest non-blocking await. */
    @Test
    fun `concurrency - parallel readers compute a binding exactly once`() = runTest {
        repeat(50) {
            val rt = KomokRuntime(dispatcher = Dispatchers.Default)
            val module = UtilsModule_Komok(rt)
            val handle = rt.rootHandle()

            val barrier = CompletableDeferred<Unit>()
            val readers = (1..32).map {
                async(Dispatchers.Default) {
                    barrier.await()
                    handle.read { module.clock() }
                }
            }
            barrier.complete(Unit)
            val results = readers.awaitAll()

            assertAll(
                { assertEquals(1, module.clockCalls.get()) },
                { assertEquals(1, module.timeSourceCalls.get()) },
                { assertEquals(1, results.toSet().size) },
            )
            rt.close()
        }
    }

    /** #17 — cancelling a reader does not cancel the shared computation. */
    @Test
    fun `cancellation isolation - cancelling a reader leaves the binding running`() = runTest {
        repeat(20) {
            val gate = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()
            val rt = KomokRuntime(dispatcher = Dispatchers.Default)
            val module = ServiceModule_Komok(rt, gate = gate, started = started)
            val handle = rt.rootHandle()

            val reader1 = async(Dispatchers.Default) { handle.read { module.gated() } }
            started.await()
            reader1.cancel()

            val reader2 = async(Dispatchers.Default) { handle.read { module.gated() } }
            gate.complete(Unit)
            val value = reader2.await()

            assertAll(
                { assertEquals("gated-value", value) },
                { assertEquals(1, module.gatedCalls.get()) },
            )
            rt.close()
        }
    }

    /** #17 — failure is memoized terminally and re-thrown to all readers. */
    @Test
    fun `cancellation isolation - failure is memoized and re-thrown`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = ServiceModule_Komok(rt)
        val handle = rt.rootHandle()

        val first = captureFailure { handle.read { module.failing() } }
        val second = captureFailure { handle.read { module.failing() } }

        // The body runs once (terminal memoization) and every reader observes the
        // failure. Object identity across `await` is not asserted: coroutines
        // stacktrace-recovery may copy the exception for the awaiting reader.
        assertAll(
            { assertInstanceOf<ServiceFailure>(first) },
            { assertInstanceOf<ServiceFailure>(second) },
            { assertEquals(first.message, second.message) },
            { assertEquals(1, module.failingCalls.get()) },
        )
        rt.close()
    }

    /**
     * Policy — a CancellationException thrown by the provider itself (not by realm close)
     * is a terminal failure like any other: memoized, re-thrown, never recomputed. Guards
     * against the cell being left as a stuck in-progress cancelled deferred.
     */
    @Test
    fun `failure policy - provider-thrown CancellationException is memoized terminally`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = ServiceModule_Komok(rt)
        val handle = rt.rootHandle()

        val first = captureFailure { handle.read { module.cancelling() } }
        val second = captureFailure { handle.read { module.cancelling() } }

        assertAll(
            { assertInstanceOf<CancellationException>(first) },
            { assertInstanceOf<CancellationException>(second) },
            { assertEquals(1, module.cancellingCalls.get()) },
        )
        rt.close()
    }

    /** #17 — one failing binding does not cancel its siblings (SupervisorJob). */
    @Test
    fun `cancellation isolation - a failing binding does not poison siblings`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = ServiceModule_Komok(rt)
        val handle = rt.rootHandle()

        val failure = captureFailure { handle.read { module.failing() } }
        val plain = handle.read { module.plain() }

        assertAll(
            { assertInstanceOf<ServiceFailure>(failure) },
            { assertEquals("plain-value", plain) },
        )
        rt.close()
    }
}

private suspend fun captureFailure(block: suspend () -> Any?): Throwable =
    runCatching { block() }.exceptionOrNull()
        ?: throw AssertionError("expected the block to fail, but it succeeded")
