package io.heapy.komok.tech.runtime.sample

import io.heapy.komok.tech.runtime.*
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
    /** #5 — graph<AppModule> builds a working graph; providers read contextless. */
    @Test
    fun `root API - graph provides root and module providers without a visible KomokContext`() = runTest {
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }

        val application = graph { application() }
        val clock = graph.read { module<UtilsModule>().clock() }

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
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }

        val first = graph.read { module<UtilsModule>().clock() }
        val second = graph.read { module<UtilsModule>().clock() }

        assertSame(first, second)
        graph.stop()
    }

    /** #2 + #4 — intra- and cross-module edges recorded through generated modules. */
    @Test
    fun `edge recording - generated modules record intra- and cross-module edges`() = runTest {
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }
        val rt = graph.handle.runtime
        val handle = graph.handle

        val application = handle.read { graph.root.application() }
        val edges = rt.edges()

        assertAll(
            { assertNotNull(application) },
            { assertTrue(edges.contains(Edge(UtilsModule.Bindings.clock.id, UtilsModule.Bindings.timeSource.id))) },
            { assertTrue(edges.contains(Edge(JdbcModule.Bindings.dataSource.id, ConfigModule.Bindings.jdbcUrl.id))) },
            { assertTrue(edges.contains(Edge(WorkerModule.Bindings.worker.id, ConfigModule.Bindings.jdbcUrl.id))) },
        )
        rt.close()
    }

    /** #7 — one interned ConfigModule: realized once, replacement invalidates both dependents. */
    @Test
    fun `interning - shared config realized once and replacement invalidates both dependents`() = runTest {
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }
        val rt = graph.handle.runtime
        val handle = graph.handle

        val rootDataSource = handle.read { graph.module<JdbcModule>().dataSource() }
        val rootWorker = handle.read { graph.module<WorkerModule>().worker() }
        val jdbcUrlCalls = graph.module<ConfigModule>().jdbcUrlCalls.get()

        val fork = handle.fork(
            mapOf(ConfigModule.Bindings.jdbcUrl.id to BindingOverride.Value("jdbc:replaced")),
        )
        val forkDataSource = fork.read { graph.module<JdbcModule>().dataSource() }
        val forkWorker = fork.read { graph.module<WorkerModule>().worker() }

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
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }

        val application = graph { application() }
        val rootClock = graph.read { module<UtilsModule>().clock() }
        val rootDataSource = graph.read { module<JdbcModule>().dataSource() }

        val fixed = fixedTimeSource()
        val fork = graph.fork {
            replace(UtilsModule.Bindings.timeSource, fixed)
        }
        val forkClock = fork.read { module<UtilsModule>().clock() }
        val forkDataSource = fork.read { module<JdbcModule>().dataSource() }

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
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }

        val rootDataSource = graph.read { module<JdbcModule>().dataSource() }
        val fork = graph.fork {
            replace(ConfigModule.Bindings.jdbcUrl, "jdbc:forked")
            replaceWith(JdbcModule.Bindings.dataSource) { Db("test:" + module<ConfigModule>().jdbcUrl(), poolSize = 7) }
        }
        val forkDataSource = fork.read { module<JdbcModule>().dataSource() }

        assertAll(
            { assertNotSame(rootDataSource, forkDataSource) },
            // the replacement body saw the forked module<ConfigModule>().jdbcUrl().
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
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }

        graph.realizeAll()
        val fixed = fixedTimeSource()
        val fork = graph.fork {
            replace(UtilsModule.Bindings.timeSource, fixed)
        }
        val rootDataSource = graph.read { module<JdbcModule>().dataSource() }
        val forkDataSource = fork.read { module<JdbcModule>().dataSource() }
        val forkClock = fork.read { module<UtilsModule>().clock() }

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
        val graph = graph<AppModule> { dispatcher = Dispatchers.Default }

        val application = graph { application() }
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
