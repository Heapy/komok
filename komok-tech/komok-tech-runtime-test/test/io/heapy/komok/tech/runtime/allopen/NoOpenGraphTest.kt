package io.heapy.komok.tech.runtime.allopen

import io.heapy.komok.tech.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class NoOpenGraphTest {
    /** A final @Module compiles and runs without all-open or a generated subclass. */
    @Test
    fun `final module without open keyword builds a working graph`() = runTest {
        val graph = graph<NoOpenModule> { dispatcher = Dispatchers.Default }

        val service = graph.read { module<NoOpenModule>().service() }

        assertAll(
            { assertEquals("configured", service.setting) },
            // The direct self-call enters the IR-instrumented binding body.
            {
                assertTrue(
                    graph.graphPayload().edges.any {
                        it.from.endsWith("NoOpenModule.service") && it.to.endsWith("NoOpenModule.setting")
                    },
                    "expected service -> setting edge from instrumented self-call",
                )
            },
        )
        graph.stop()
    }
}
