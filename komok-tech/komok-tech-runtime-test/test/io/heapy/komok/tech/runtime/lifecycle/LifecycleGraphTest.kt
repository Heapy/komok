package io.heapy.komok.tech.runtime.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertInstanceOf

class LifecycleGraphTest {
    /** #14 — generated graph runs @Startup deps-first and @Shutdown in strict reverse. */
    @Test
    fun `lifecycle order - generated graph runs startup deps-first and shutdown reverse`() = runTest {
        val graph = createLifecycleModuleGraph { dispatcher = Dispatchers.Default }

        graph.start()
        graph.stop()
        val events = graph.modules.logModule.events()

        assertEquals(
            listOf("config.start", "app.start", "app.stop", "config.stop"),
            events,
        )
    }

    /**
     * #18 — a failing @Startup hook in a generated graph rolls back the already-started
     * modules in reverse (compensation), fails fast, and a subsequent stop() adds no
     * second teardown.
     */
    @Test
    fun `generated startup compensation - failing hook rolls back and fails fast`() = runTest {
        val graph = createFailingBootModuleGraph { dispatcher = Dispatchers.Default }

        val failure = runCatching { graph.start() }.exceptionOrNull()
        val events = graph.modules.logModule.events().toList()
        graph.stop()
        val eventsAfterStop = graph.modules.logModule.events().toList()

        assertAll(
            { assertInstanceOf<IllegalStateException>(failure) },
            // config started; failing module threw; only config is compensated.
            { assertEquals(listOf("config.start", "failing.start", "config.stop"), events) },
            // compensation was the teardown — stop() must not re-fire any hook.
            { assertEquals(events, eventsAfterStop) },
        )
    }

    /** A fork releases its overlay on stop() but does not re-run @Shutdown hooks — those
     *  are a root concern, so the recorded events stay exactly the root's startup+shutdown. */
    @Test
    fun `fork stop - releases overlay without re-running shutdown hooks`() = runTest {
        val graph = createLifecycleModuleGraph { dispatcher = Dispatchers.Default }

        graph.start()
        val fork = graph.fork { }
        fork.stop()
        graph.stop()
        val events = graph.modules.logModule.events()

        assertEquals(
            listOf("config.start", "app.start", "app.stop", "config.stop"),
            events,
        )
    }
}
