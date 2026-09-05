package io.heapy.komok.tech.runtime.sample

import io.heapy.komok.tech.runtime.BindingOverride
import io.heapy.komok.tech.runtime.Edge
import io.heapy.komok.tech.runtime.KomokRuntime
import io.heapy.komok.tech.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GeneratedGraphTest {
    /** #5 — createAppModuleGraph builds a working graph; providers read contextless. */
    @Test
    fun `root API - graph provides root and module providers without a visible KomokContext`() = runTest {
        val graph = createAppModuleGraph { dispatcher = Dispatchers.Default }

        val application = graph.application()
        val clock = graph.modules.utilsModule.clock()

        assertAll(
            { assertNotNull(application) },
            { assertNotNull(clock) },
            // root provider and module handle resolve the same memoized clock.
            { assertSame(application.clock, clock) },
        )
        graph.stop()
    }

    /** #1 — memoization through the generated graph. */
    @Test
    fun `memoization - repeated read through generated graph returns same instance`() = runTest {
        val graph = createAppModuleGraph { dispatcher = Dispatchers.Default }

        val first = graph.modules.utilsModule.clock()
        val second = graph.modules.utilsModule.clock()

        assertSame(first, second)
        graph.stop()
    }

    /** #2 + #4 — intra- and cross-module edges recorded through generated modules. */
    @Test
    fun `edge recording - generated modules record intra- and cross-module edges`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val dag = AppModuleDag(rt)
        val handle = rt.rootHandle()

        val application = handle.read { dag.appModule.application() }
        val edges = rt.edges()

        assertAll(
            { assertNotNull(application) },
            { assertTrue(edges.contains(Edge(UtilsModuleKeys.clock, UtilsModuleKeys.timeSource))) },
            { assertTrue(edges.contains(Edge(JdbcModuleKeys.dataSource, ConfigModuleKeys.jdbcUrl))) },
            { assertTrue(edges.contains(Edge(WorkerModuleKeys.worker, ConfigModuleKeys.jdbcUrl))) },
        )
        rt.close()
    }

    /** #7 — one interned ConfigModule: realized once, replacement invalidates both dependents. */
    @Test
    fun `interning - shared config realized once and replacement invalidates both dependents`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val dag = AppModuleDag(rt)
        val handle = rt.rootHandle()

        val rootDataSource = handle.read { dag.jdbcModule.dataSource() }
        val rootWorker = handle.read { dag.workerModule.worker() }
        val jdbcUrlCalls = dag.configModule.jdbcUrlCalls.get()

        val fork = handle.fork(
            mapOf(ConfigModuleKeys.jdbcUrl to BindingOverride.Value("jdbc:replaced")),
        )
        val forkDataSource = fork.read { dag.jdbcModule.dataSource() }
        val forkWorker = fork.read { dag.workerModule.worker() }

        assertAll(
            // both dependents share one ConfigModule => one binding, realized once.
            { assertEquals(1, jdbcUrlCalls) },
            { assertNotSame(rootDataSource, forkDataSource) },
            { assertNotSame(rootWorker, forkWorker) },
            { assertEquals("jdbc:replaced", forkDataSource.url) },
            { assertEquals("jdbc:replaced", forkWorker.url) },
        )
        rt.close()
    }

    /** #9 + #16 — fork semantics through generated graph: cone recomputed, non-cone shared. */
    @Test
    fun `fork semantics - cone recomputed and non-cone resource shared through generated graph`() = runTest {
        val graph = createAppModuleGraph { dispatcher = Dispatchers.Default }

        val application = graph.application()
        val rootClock = graph.modules.utilsModule.clock()
        val rootDataSource = graph.modules.jdbcModule.dataSource()

        val fixed = fixedTimeSource()
        val fork = graph.fork {
            utilsModule { replaceTimeSource(fixed) }
        }
        val forkClock = fork.modules.utilsModule.clock()
        val forkDataSource = fork.modules.jdbcModule.dataSource()

        assertAll(
            { assertNotNull(application) },
            { assertNotSame(rootClock, forkClock) },
            { assertSame(fixed.clock, forkClock) },
            { assertSame(rootDataSource, forkDataSource) },
        )
        fork.stop()
        graph.stop()
    }

    /** #8 — replaceXWith body runs lazily in the fork and reads a forked dependency. */
    @Test
    fun `provider replacement - typed replaceWith reads forked dependency through provider scope`() = runTest {
        val graph = createAppModuleGraph { dispatcher = Dispatchers.Default }

        val rootDataSource = graph.modules.jdbcModule.dataSource()
        val fork = graph.fork {
            configModule { replaceJdbcUrl("jdbc:forked") }
            jdbcModule {
                replaceDataSourceWith { Db("test:" + configModule.jdbcUrl(), poolSize = 7) }
            }
        }
        val forkDataSource = fork.modules.jdbcModule.dataSource()

        assertAll(
            { assertNotSame(rootDataSource, forkDataSource) },
            // the replacement body saw the forked configModule.jdbcUrl().
            { assertEquals("test:jdbc:forked", forkDataSource.url) },
            { assertEquals(7, forkDataSource.poolSize) },
        )
        fork.stop()
        graph.stop()
    }

    /**
     * Policy B (realize-then-fork) — realizeAll() completes every static binding, so a
     * fork created afterwards shares all non-cone values by reference without any
     * explicit provider reads in between.
     */
    @Test
    fun `realizeAll - fork after realizeAll shares non-cone values by reference`() = runTest {
        val graph = createAppModuleGraph { dispatcher = Dispatchers.Default }

        graph.realizeAll()
        val fixed = fixedTimeSource()
        val fork = graph.fork {
            utilsModule { replaceTimeSource(fixed) }
        }
        val rootDataSource = graph.modules.jdbcModule.dataSource()
        val forkDataSource = fork.modules.jdbcModule.dataSource()
        val forkClock = fork.modules.utilsModule.clock()

        assertAll(
            // completed by realizeAll *before* the fork => shared, not recomputed.
            { assertSame(rootDataSource, forkDataSource) },
            { assertSame(fixed.clock, forkClock) },
            // realizeAll recorded the full execution-fact edge set, root provider included.
            {
                assertTrue(
                    graph.graphPayload().edges.any { it.from.endsWith("AppModule.application") },
                    "expected application edges after realizeAll",
                )
            },
        )
        fork.stop()
        graph.stop()
    }

    /** P6 — generated graphPayload() combines the static module graph with runtime edges. */
    @Test
    fun `graph payload - generated payload has static modules and runtime edges`() = runTest {
        val graph = createAppModuleGraph { dispatcher = Dispatchers.Default }

        val application = graph.application()
        val payload = graph.graphPayload()
        val json = payload.toJson()

        assertAll(
            { assertNotNull(application) },
            // full static module graph (compile-time truth), AppModule depends on three modules.
            { assertTrue(payload.modules.any { it.fqn.endsWith(".AppModule") && it.dependencies.size == 3 }) },
            { assertTrue(payload.modules.any { it.fqn.endsWith(".ConfigModule") && it.dependencies.isEmpty() }) },
            // binding edge (execution fact) recorded after realization.
            {
                assertTrue(
                    payload.edges.any {
                        it.from.endsWith("JdbcModule.dataSource") && it.to.endsWith("ConfigModule.jdbcUrl")
                    },
                )
            },
            { assertTrue(json.startsWith("{\"modules\":[")) },
        )
        graph.stop()
    }
}

private fun fixedTimeSource(): TimeSource =
    TimeSource(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
