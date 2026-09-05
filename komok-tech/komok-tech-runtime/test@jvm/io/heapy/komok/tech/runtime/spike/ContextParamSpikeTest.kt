package io.heapy.komok.tech.runtime.spike

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * P0 spike: re-confirms, under this repo's exact compiler flags and
 * `allWarningsAsErrors` (originally spiked on kotlinc 2.3.21; keeps re-verifying on every
 * toolchain bump), the linchpin compiler mechanics the whole runtime design rests on:
 *
 *  (a) a `KomokContext` context-parameter is captured *lexically* by lambdas passed
 *      to `async`, so nested provider-calls and child coroutines see the entered context;
 *  (b) the context-parameter survives suspend-resume on a different dispatcher/thread;
 *  (c) a cycle closed through a coroutine boundary is still caught via the caller-stack;
 *  (d) the whole thing compiles cleanly with the `currentSpikeContext()` bridge — no
 *      context-parameter-name-mismatch warning — and a `suspend` leaf with no suspend
 *      call in its body is NOT a compiler warning (redundant-suspend is IDE-only).
 *
 * The real runtime (P1) replaces this throwaway mini-binding.
 */
class ContextParamSpikeTest {
    /** Mini-`KomokContext`: just the caller-stack for cycle detection. */
    private class SpikeContext(
        val callerStack: List<String>,
    ) {
        fun enter(id: String): SpikeContext =
            SpikeContext(callerStack + id)
    }

    private class SpikeCycleException(
        path: List<String>,
    ) : RuntimeException("cycle: ${path.joinToString(" -> ")}")

    /** Bridge: lets context-parameter providers be written `context(_: SpikeContext)`. */
    context(ctx: SpikeContext)
    private fun currentSpikeContext(): SpikeContext = ctx

    /**
     * Mini-`binding`: runs [body] on [scope] (NOT the caller's coroutine), passing the
     * entered context positionally — exactly the runtime's `context(entered) { body() }`.
     */
    context(_: SpikeContext)
    private suspend fun <T> spikeBinding(
        scope: CoroutineScope,
        id: String,
        body: suspend context(SpikeContext) () -> T,
    ): T {
        val current = currentSpikeContext()
        if (id in current.callerStack) {
            throw SpikeCycleException(current.callerStack + id)
        }
        val entered = current.enter(id)
        return scope.async { body(entered) }.await()
    }

    /** Entry helper: invokes a context-lambda positionally (codebase convention). */
    private suspend fun <T> withSpikeContext(
        ctx: SpikeContext,
        body: suspend context(SpikeContext) () -> T,
    ): T = body(ctx)

    /** (d) redundant-suspend: leaf `suspend fun` with no suspend call in body. */
    context(_: SpikeContext)
    private suspend fun leaf(): Int = 42

    @Test
    fun `context parameter is captured lexically through async`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            // clock() body calls leaf() — a nested provider-call that must resolve the
            // SpikeContext from the lexically-captured `entered`, not the caller's.
            val result = withSpikeContext(SpikeContext(emptyList())) {
                spikeBinding(scope, "clock") {
                    leaf() + 1
                }
            }
            assertEquals(43, result)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `context parameter survives resume on another dispatcher`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val stack = withSpikeContext(SpikeContext(emptyList())) {
                spikeBinding(scope, "crossThread") {
                    withContext(Dispatchers.IO) {
                        // resumed on an IO thread; the context-parameter must still be visible.
                        currentSpikeContext().callerStack.joinToString(",")
                    }
                }
            }
            assertEquals("crossThread", stack)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `cycle closed through a coroutine boundary is caught`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val ex = assertThrows<SpikeCycleException> {
                withSpikeContext(SpikeContext(emptyList())) {
                    spikeBinding(scope, "self") {
                        // re-enter the same id via a fresh async that captures `entered`.
                        scope.async { spikeBinding(scope, "self") { 0 } }.await()
                    }
                }
            }
            assertEquals("cycle: self -> self", ex.message)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `concurrent readers each capture context independently`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val results = withSpikeContext(SpikeContext(emptyList())) {
                (1..16).map { n ->
                    scope.async {
                        // each child coroutine lexically captures the SpikeContext.
                        spikeBinding(scope, "leaf-$n") { leaf() }
                    }
                }.awaitAll()
            }
            assertAll(
                { assertEquals(16, results.size) },
                { assertEquals(List(16) { 42 }, results) },
            )
        } finally {
            scope.cancel()
        }
    }
}
