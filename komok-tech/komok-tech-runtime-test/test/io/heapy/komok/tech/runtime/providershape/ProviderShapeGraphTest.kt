package io.heapy.komok.tech.runtime.providershape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ProviderShapeGraphTest {
    /** Provider-shape filtering: helpers are callable but not addressable bindings. */
    @Test
    fun `only public suspend value providers become generated bindings`() = runTest {
        val graph = createProviderShapeModuleGraph { dispatcher = Dispatchers.Default }

        val service = graph.modules.providerShapeModule.service()
        val module = graph
            .graphPayload()
            .modules
            .single { it.fqn.endsWith(".ProviderShapeModule") }

        assertAll(
            { assertEquals("private:param:non-suspend", service.value) },
            { assertEquals(listOf("io.heapy.komok.tech.runtime.providershape.ProviderShapeModule.service"), module.bindings) },
            { assertTrue(graph.graphPayload().edges.isEmpty()) },
            { assertFalse(module.bindings.any { it.endsWith(".privateHelper") }) },
            { assertFalse(module.bindings.any { it.endsWith(".publicHelperWithValueParam") }) },
            { assertFalse(module.bindings.any { it.endsWith(".nonSuspendHelper") }) },
            { assertFalse(module.bindings.any { it.endsWith(".unitHelper") }) },
        )
        graph.stop()
    }
}
