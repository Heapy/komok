package io.heapy.komok.tech.runtime.compiler.sample

import io.heapy.komok.tech.runtime.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DecoratorTest {
    @Test fun `production and replacement bodies are decorated while terminal values remain bare`() = runTest {
        val graph = graph<DecoratedModule>()
        try {
            val original = graph { flaky() }
            assertEquals("ok", original.call())
            assertEquals(2, original.calls)
            assertEquals("implementation", original.default())
            val fork = graph.fork { replaceWith(DecoratedModule.Bindings.flaky) { FlakyImplementation() } }
            assertEquals("ok", fork { flaky() }.call())
            fork.stop()
            val bare = FlakyImplementation()
            val terminal = graph.fork { replace(DecoratedModule.Bindings.flaky, bare) }
            assertSame(bare, terminal { flaky() })
            assertThrows(IllegalStateException::class.java) { runTest { val _ = terminal { flaky() }.call() } }
            terminal.stop()
        } finally { graph.stop() }
    }
}
