package io.heapy.komok.tech.runtime.ksp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertInstanceOf

/**
 * Verifies the module-graph topological sort that detects compile-time cycles (#11) and
 * fixes the edge direction the DAG construction order depends on.
 */
class TopologicalSortTest {
    @Test
    fun `acyclic graph sorts root-first and reverses to deps-first`() {
        // app -> [utils, jdbc]; jdbc -> [config]; utils, config -> []
        val graph = mapOf(
            "app" to listOf("utils", "jdbc"),
            "jdbc" to listOf("config"),
            "utils" to emptyList(),
            "config" to emptyList(),
        )

        val rootFirst = graph.topologicalSort()
        val depsFirst = rootFirst.reversed()

        assertAll(
            { assertEquals("app", rootFirst.first()) },
            // every dependency comes before its dependent in deps-first order.
            { assertTrue(depsFirst.indexOf("config") < depsFirst.indexOf("jdbc")) },
            { assertTrue(depsFirst.indexOf("jdbc") < depsFirst.indexOf("app")) },
            { assertTrue(depsFirst.indexOf("utils") < depsFirst.indexOf("app")) },
        )
    }

    @Test
    fun `cyclic graph throws ModuleCycleException`() {
        val graph = mapOf(
            "a" to listOf("b"),
            "b" to listOf("a"),
        )

        val failure = runCatching { graph.topologicalSort() }.exceptionOrNull()

        assertInstanceOf<ModuleCycleException>(failure)
    }
}
