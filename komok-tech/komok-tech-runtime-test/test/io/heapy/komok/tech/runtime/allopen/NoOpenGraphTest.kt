package io.heapy.komok.tech.runtime.allopen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class NoOpenGraphTest {
    /** A @Module with no `open` keyword compiles and runs: all-open opened it, KSP didn't care. */
    @Test
    fun `all-open module without open keyword builds a working graph`() = runTest {
        val graph = createNoOpenModuleGraph { dispatcher = Dispatchers.Default }

        val service = graph.modules.noOpenModule.service()

        assertAll(
            { assertEquals("configured", service.setting) },
            // service() -> setting() recorded an edge => the self-call dispatched virtually
            // to the generated override => all-open made setting() overridable.
            {
                assertTrue(
                    graph.graphPayload().edges.any {
                        it.from.endsWith("NoOpenModule.service") && it.to.endsWith("NoOpenModule.setting")
                    },
                    "expected service -> setting edge from virtual self-call",
                )
            },
        )
        graph.stop()
    }
}
