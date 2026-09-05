package io.heapy.komok.tech.runtime

import io.heapy.komok.tech.runtime.fixtures.CloseableModuleKeys
import io.heapy.komok.tech.runtime.fixtures.CloseableModule_Komok
import io.heapy.komok.tech.runtime.fixtures.CloseableResource
import io.heapy.komok.tech.runtime.fixtures.LateCloseModule_Komok
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertInstanceOf

class LifecycleTest {
    private class BootFailure : RuntimeException("startup hook failed")

    /** #14 — @Startup runs deps-first; @Shutdown is the strict reverse. */
    @Test
    fun `lifecycle order - startup deps-first and shutdown strict reverse`() = runTest {
        val log = mutableListOf<String>()
        val registry = LifecycleRegistry().apply {
            register("config", LifecyclePhase.STARTUP) { log.add("config.start") }
            register("config", LifecyclePhase.SHUTDOWN) { log.add("config.stop") }
            register("jdbc", LifecyclePhase.STARTUP) { log.add("jdbc.start") }
            register("jdbc", LifecyclePhase.SHUTDOWN) { log.add("jdbc.stop") }
            register("app", LifecyclePhase.STARTUP) { log.add("app.start") }
            register("app", LifecyclePhase.SHUTDOWN) { log.add("app.stop") }
        }
        val lifecycle = Lifecycle(registry)

        lifecycle.startup()
        lifecycle.shutdown()

        assertEquals(
            listOf(
                "config.start", "jdbc.start", "app.start",
                "app.stop", "jdbc.stop", "config.stop",
            ),
            log,
        )
    }

    /** #18 — a failing @Startup hook rolls back started modules in reverse, then fails fast. */
    @Test
    fun `startup compensation - failure rolls back started modules and stops`() = runTest {
        val log = mutableListOf<String>()
        val registry = LifecycleRegistry().apply {
            register("a", LifecyclePhase.STARTUP) { log.add("a.start") }
            register("a", LifecyclePhase.SHUTDOWN) { log.add("a.stop") }
            register("b", LifecyclePhase.STARTUP) { log.add("b.start"); throw BootFailure() }
            register("b", LifecyclePhase.SHUTDOWN) { log.add("b.stop") }
            register("c", LifecyclePhase.STARTUP) { log.add("c.start") }
            register("c", LifecyclePhase.SHUTDOWN) { log.add("c.stop") }
        }
        val lifecycle = Lifecycle(registry)

        val failure = captureFailure { lifecycle.startup() }

        assertAll(
            { assertInstanceOf<BootFailure>(failure) },
            // a started; b started then threw; c never started; only a is compensated.
            { assertEquals(listOf("a.start", "b.start", "a.stop"), log) },
        )
    }

    /** A failed startup compensates once; a later shutdown() must not be a second teardown. */
    @Test
    fun `startup compensation - subsequent shutdown does not re-run hooks`() = runTest {
        val log = mutableListOf<String>()
        val registry = LifecycleRegistry().apply {
            register("a", LifecyclePhase.STARTUP) { log.add("a.start") }
            register("a", LifecyclePhase.SHUTDOWN) { log.add("a.stop") }
            register("b", LifecyclePhase.STARTUP) { log.add("b.start"); throw BootFailure() }
            register("b", LifecyclePhase.SHUTDOWN) { log.add("b.stop") }
        }
        val lifecycle = Lifecycle(registry)

        val failure = captureFailure { lifecycle.startup() }
        lifecycle.shutdown()

        assertAll(
            { assertInstanceOf<BootFailure>(failure) },
            // compensation was the teardown; shutdown() added nothing.
            { assertEquals(listOf("a.start", "b.start", "a.stop"), log) },
        )
    }

    /** startup/shutdown are idempotent — a redundant stop() never re-fires @Shutdown hooks. */
    @Test
    fun `lifecycle idempotent - repeated startup and shutdown run hooks once`() = runTest {
        val log = mutableListOf<String>()
        val registry = LifecycleRegistry().apply {
            register("a", LifecyclePhase.STARTUP) { log.add("a.start") }
            register("a", LifecyclePhase.SHUTDOWN) { log.add("a.stop") }
        }
        val lifecycle = Lifecycle(registry)

        lifecycle.startup()
        lifecycle.startup()
        lifecycle.shutdown()
        lifecycle.shutdown()

        assertEquals(listOf("a.start", "a.stop"), log)
    }

    /** #19 — fork.close closes its overlay AutoCloseable values, not parent-shared ones. */
    @Test
    fun `per-realm close - closes overlay resources but not parent-shared`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = CloseableModule_Komok(rt)
        val root = rt.rootHandle()

        val rootShared = root.read { module.shared() }
        val rootCone = root.read { module.coneResource() }

        val fork = root.fork(
            mapOf(CloseableModuleKeys.flag to BindingOverride.Value(false)),
        )
        val forkCone = fork.read { module.coneResource() }
        val forkShared = fork.read { module.shared() }
        fork.close()

        assertAll(
            { assertNotSame(rootCone, forkCone) },
            { assertSame(rootShared, forkShared) },
            { assertTrue(forkCone.closed) },
            { assertFalse(rootShared.closed) },
            { assertFalse(rootCone.closed) },
        )
        rt.close()
    }

    /**
     * Close/completion race — a binding body past its last suspension point completes
     * after the close sweep; its value must still be released (self-close path).
     */
    @Test
    fun `realm close - value completing after close is still released`() = runTest {
        val created = CompletableDeferred<CloseableResource>()
        val release = CountDownLatch(1)
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = LateCloseModule_Komok(rt, created, release)
        val root = rt.rootHandle()
        val fork = root.fork(emptyMap())

        val reader = async(Dispatchers.Default) { fork.read { module.resource() } }
        val resource = created.await()
        fork.close()
        release.countDown()
        // await unblocks only after the body finished, which happens-after the self-close.
        val outcome = runCatching { reader.await() }

        assertAll(
            { assertTrue(outcome.isFailure) },
            { assertTrue(resource.closed) },
        )
        rt.close()
    }

    /** rt.close() releases root-realm resources, not only cancels the job. */
    @Test
    fun `runtime close - closes root-realm AutoCloseable values`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = CloseableModule_Komok(rt)
        val root = rt.rootHandle()

        val resource = root.read { module.shared() }
        rt.close()

        assertTrue(resource.closed)
    }
}

private suspend fun captureFailure(block: suspend () -> Any?): Throwable =
    runCatching { block() }.exceptionOrNull()
        ?: throw AssertionError("expected the block to fail, but it succeeded")
