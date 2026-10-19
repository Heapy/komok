package io.heapy.komok.tech.runtime.rootcontext

import io.heapy.komok.tech.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootContextGraphTest {
    /** The builder's rootContext element is visible inside provider realization. */
    @Test
    fun `builder rootContext is present in the provider realization context`() = runTest {
        val graph = graph<RootContextModule> {
            dispatcher = Dispatchers.Default
            rootContext = TenantElement("acme")
        }

        val tenant = graph { tenant() }

        assertEquals("acme", tenant)
        graph.stop()
    }

    /** Without a configured rootContext the realization context has no extra elements. */
    @Test
    fun `default rootContext is empty`() = runTest {
        val graph = graph<RootContextModule> { dispatcher = Dispatchers.Default }

        val tenant = graph { tenant() }

        assertEquals("absent", tenant)
        graph.stop()
    }
}
