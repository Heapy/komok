package io.heapy.komok.tech.runtime.compiler.sample

import io.heapy.komok.tech.runtime.BindingId
import io.heapy.komok.tech.runtime.BindingOverride
import io.heapy.komok.tech.runtime.Edge
import io.heapy.komok.tech.runtime.KomokRuntime
import io.heapy.komok.tech.runtime.ProviderSignature
import io.heapy.komok.tech.runtime.graph
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class FinalModuleTest {
    @Test
    fun `compiler creates a typed graph and binding keys`() = runTest {
        val graph = graph<FinalModule>()
        try {
            val original = graph { root() }
            assertSame(original, graph[FinalModule.Bindings.root])
            assertSame(original[0], graph.read { module<FinalModule>().leaf() })
            val replacement = Any()
            val fork = graph.fork { replace(FinalModule.Bindings.leaf, replacement) }
            assertSame(replacement, fork { root() }[0])
            assertSame(original, graph { root() })
            fork.stop()
        } finally {
            graph.stop()
        }
    }

    @Test
    fun `final providers memoize self calls and participate in persistent forks`() = runTest {
        val runtime = KomokRuntime()
        try {
            val module = FinalModule()
            val root = runtime.rootHandle()
            val first = root.read { module.root() }
            val second = root.read { module.root() }
            val owner = "io.heapy.komok.tech.runtime.compiler.sample.FinalModule"
            val leaf = BindingId(owner, ProviderSignature("leaf"))
            val rootId = BindingId(owner, ProviderSignature("root"))
            assertSame(first, second)
            assertSame(first[0], first[1])
            assertEquals(1, module.creations)
            assertEquals(setOf(Edge(rootId, leaf)), runtime.edges())
            assertTrue(Modifier.isFinal(FinalModule::class.java.modifiers))
            assertEquals(Any::class.java, FinalModule::class.java.superclass)
            val replacement = Any()
            val fork = root.fork(mapOf(leaf to BindingOverride.Value(replacement)))
            val forkValue = fork.read { module.root() }
            assertNotSame(first, forkValue)
            assertSame(replacement, forkValue[0])
            assertSame(first, root.read { module.root() })
            fork.close()
        } finally {
            runtime.close()
        }
    }
}
