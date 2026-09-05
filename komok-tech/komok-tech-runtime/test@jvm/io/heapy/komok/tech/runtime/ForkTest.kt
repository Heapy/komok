package io.heapy.komok.tech.runtime

import io.heapy.komok.tech.runtime.fixtures.ConfigModuleKeys
import io.heapy.komok.tech.runtime.fixtures.ConfigModule_Komok
import io.heapy.komok.tech.runtime.fixtures.FakeDataSource
import io.heapy.komok.tech.runtime.fixtures.JdbcModuleKeys
import io.heapy.komok.tech.runtime.fixtures.JdbcModule_Komok
import io.heapy.komok.tech.runtime.fixtures.ServiceModule_Komok
import io.heapy.komok.tech.runtime.fixtures.UtilsModuleKeys
import io.heapy.komok.tech.runtime.fixtures.UtilsModule_Komok
import io.heapy.komok.tech.runtime.fixtures.WorkerModuleKeys
import io.heapy.komok.tech.runtime.fixtures.WorkerModule_Komok
import io.heapy.komok.tech.runtime.fixtures.fixedTimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull

class ForkTest {
    /** #6 — fork returns a distinct child handle that does not share cone values. */
    @Test
    fun `fork API - child handle does not share cone values with root`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        val rootClock = root.read { utils.clock() }
        val fork = root.fork(
            mapOf(UtilsModuleKeys.timeSource to BindingOverride.Value(fixedTimeSource())),
        )
        val forkClock = fork.read { utils.clock() }
        val rootClockAgain = root.read { utils.clock() }

        assertAll(
            { assertNotSame(root, fork) },
            { assertNotSame(rootClock, forkClock) },
            { assertSame(rootClock, rootClockAgain) },
        )
        rt.close()
    }

    /**
     * Regression — fork resolution is local-first. A non-cone binding realized in the child
     * before the parent completes it must keep the child's own instance even after the parent
     * later realizes the same binding; the parent value is shared only when the child holds
     * none of its own. Guards against parent-first resolution silently swapping the instance.
     */
    @Test
    fun `fork resolution - local value wins when parent completes a non-cone binding later`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        // Fork before the parent realizes anything; empty overrides => empty cone, so
        // timeSource is a non-cone binding eligible for parent sharing.
        val fork = root.fork(emptyMap())

        val forkTimeSource = fork.read { utils.timeSource() }   // child computes its own
        val rootTimeSource = root.read { utils.timeSource() }   // parent computes later
        val forkTimeSourceAgain = fork.read { utils.timeSource() }

        assertAll(
            { assertNotSame(rootTimeSource, forkTimeSource) },
            { assertSame(forkTimeSource, forkTimeSourceAgain) },
            // each realm realized timeSource exactly once: two independent computations.
            { assertEquals(2, utils.timeSourceCalls.get()) },
        )
        rt.close()
    }

    /** #7 — one shared module = one binding; replacing it invalidates both dependents. */
    @Test
    fun `module identity - shared module is one binding and replacement invalidates dependents`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val worker = WorkerModule_Komok(rt, config)
        val root = rt.rootHandle()

        val rootDs = root.read { jdbc.dataSource() }
        val rootWorker = root.read { worker.worker() }

        val fork = root.fork(
            mapOf(ConfigModuleKeys.jdbcUrl to BindingOverride.Value("jdbc:replaced")),
        )
        val forkDs = fork.read { jdbc.dataSource() }
        val forkWorker = fork.read { worker.worker() }

        assertAll(
            // one ConfigModule instance => one BindingId => jdbcUrl realized once.
            { assertEquals(1, config.jdbcUrlCalls.get()) },
            {
                assertEquals(
                    setOf(
                        Edge(JdbcModuleKeys.dataSource, ConfigModuleKeys.jdbcUrl),
                        Edge(WorkerModuleKeys.worker, ConfigModuleKeys.jdbcUrl),
                    ),
                    rt.edges(),
                )
            },
            // replacing the shared binding recomputes both dependents in the fork.
            { assertNotSame(rootDs, forkDs) },
            { assertNotSame(rootWorker, forkWorker) },
            { assertEquals("jdbc:replaced", forkDs.url) },
            { assertEquals("jdbc:replaced", forkWorker.url) },
        )
        rt.close()
    }

    /** #8 — a provider-body replacement runs lazily in the child and sees forked deps. */
    @Test
    fun `provider replacement - thunk runs lazily, sees forked deps, records edge`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val root = rt.rootHandle()

        val rootDs = root.read { jdbc.dataSource() }
        val edgesAfterRealize = rt.edges()

        val fork = root.fork(
            mapOf(
                ConfigModuleKeys.jdbcUrl to BindingOverride.Value("jdbc:forked"),
                JdbcModuleKeys.dataSource to BindingOverride.Provider<FakeDataSource> {
                    FakeDataSource("test:" + config.jdbcUrl())
                },
            ),
        )

        // fork {} did not read the graph: no new edges from building the fork.
        assertEquals(edgesAfterRealize, rt.edges())

        val forkDs = fork.read { jdbc.dataSource() }

        assertAll(
            // the replacement body executed in the child realm and saw the forked config.
            { assertEquals("test:jdbc:forked", forkDs.url) },
            { assertNotSame(rootDs, forkDs) },
            {
                assertTrue(
                    rt.edges().contains(
                        Edge(JdbcModuleKeys.dataSource, ConfigModuleKeys.jdbcUrl),
                    ),
                )
            },
        )
        rt.close()
    }

    /** #9 — realize-then-fork: cone recomputed, non-cone shared, original immutable. */
    @Test
    fun `fork semantics - cone recomputed, non-cone shared by reference, original immutable`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        val rootClock = root.read { utils.clock() }
        val rootDataSource = root.read { jdbc.dataSource() }

        val fixed = fixedTimeSource()
        val fork = root.fork(
            mapOf(UtilsModuleKeys.timeSource to BindingOverride.Value(fixed)),
        )
        val forkClock = fork.read { utils.clock() }
        val forkDataSource = fork.read { jdbc.dataSource() }
        val rootClockAgain = root.read { utils.clock() }

        assertAll(
            { assertNotSame(rootClock, forkClock) },
            { assertSame(fixed.clock, forkClock) },
            { assertSame(rootDataSource, forkDataSource) },
            { assertSame(rootClock, rootClockAgain) },
        )
        rt.close()
    }

    /** #16 — non-cone resource is shared, not recreated; fork.close leaves it intact. */
    @Test
    fun `no double instantiation - non-cone resource shared and untouched by fork close`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        val rootDataSource = root.read { jdbc.dataSource() }
        val fork = root.fork(
            mapOf(UtilsModuleKeys.timeSource to BindingOverride.Value(fixedTimeSource())),
        )
        val forkDataSource = fork.read { jdbc.dataSource() }
        fork.close()
        val rootDataSourceAfter = root.read { jdbc.dataSource() }

        assertAll(
            { assertSame(rootDataSource, forkDataSource) },
            { assertSame(rootDataSource, rootDataSourceAfter) },
        )
        rt.close()
    }

    /**
     * Nested forks — a non-cone binding completed in the root is shared by reference all
     * the way down the overlay chain, while each fork recomputes its own cone. Guards
     * against one-level parent checks that recompute (double-instantiate) grandparent
     * resources in a fork-of-fork.
     */
    @Test
    fun `nested fork - root-completed non-cone binding is shared through the chain`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        val rootDataSource = root.read { jdbc.dataSource() }
        val rootClock = root.read { utils.clock() }

        val fork1 = root.fork(
            mapOf(UtilsModuleKeys.timeSource to BindingOverride.Value(fixedTimeSource())),
        )
        val fork2 = fork1.fork(
            mapOf(UtilsModuleKeys.timeSource to BindingOverride.Value(fixedTimeSource())),
        )
        val fork1Clock = fork1.read { utils.clock() }
        val fork2Clock = fork2.read { utils.clock() }
        val fork2DataSource = fork2.read { jdbc.dataSource() }

        assertAll(
            // dataSource is outside every cone and completed in the root: one instance.
            { assertSame(rootDataSource, fork2DataSource) },
            // clock is in each fork's cone: every realm has its own.
            { assertNotSame(rootClock, fork1Clock) },
            { assertNotSame(fork1Clock, fork2Clock) },
            { assertNotSame(rootClock, fork2Clock) },
        )
        rt.close()
    }

    /**
     * Nested forks — a grandchild that computes a binding its parent overrode but never
     * realized still applies the inherited override: the middle realm's cone blocks
     * sharing the root's stale value, and the merged override map supplies the
     * replacement. Guards against computing with the original body in a fork-of-fork.
     */
    @Test
    fun `nested fork - inherited override applies when the grandchild recomputes`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val root = rt.rootHandle()

        val rootDataSource = root.read { jdbc.dataSource() }

        // fork1 replaces jdbcUrl but never reads anything; fork2 adds nothing on top.
        val fork1 = root.fork(
            mapOf(ConfigModuleKeys.jdbcUrl to BindingOverride.Value("jdbc:replaced")),
        )
        val fork2 = fork1.fork(emptyMap())
        val fork2DataSource = fork2.read { jdbc.dataSource() }
        val rootDataSourceAgain = root.read { jdbc.dataSource() }

        assertAll(
            { assertEquals("jdbc:replaced", fork2DataSource.url) },
            { assertNotSame(rootDataSource, fork2DataSource) },
            { assertSame(rootDataSource, rootDataSourceAgain) },
        )
        rt.close()
    }

    /** #19 — closing a fork cancels only its own in-flight bindings; parent is unaffected. */
    @Test
    fun `per-realm close - closing a fork cancels only its own in-flight bindings`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val service = ServiceModule_Komok(rt, gate = gate, started = started)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        val fork = root.fork(emptyMap())
        val forkReader = async(Dispatchers.Default) { fork.read { service.gated() } }
        started.await()
        fork.close()
        val forkOutcome = runCatching { forkReader.await() }

        val rootClock = root.read { utils.clock() }

        assertAll(
            { assertTrue(forkOutcome.isFailure) },
            { assertNotNull(rootClock) },
        )
        gate.complete(Unit)
        rt.close()
    }
}
