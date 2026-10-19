package io.heapy.komok.tech.runtime.compiler.sample

import io.heapy.komok.tech.runtime.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ServiceTest {
    @Test fun `interface providers autowire and record edges for fork invalidation`() = runTest {
        val graph = graph<NetworkModule>()
        try {
            val original = graph[NetworkModule.Bindings.endpoint]
            assertEquals(Endpoint(Host("localhost")), original)
            assertSame(original, graph[NetworkModule.Bindings.endpoint])
            val fork = graph.fork { replace(NetworkModule.Bindings.host, Host("replacement")) }
            assertEquals(Endpoint(Host("replacement")), fork[NetworkModule.Bindings.endpoint])
            assertSame(original, graph[NetworkModule.Bindings.endpoint])
            fork.stop()
        } finally { graph.stop() }
    }

    @Test fun `classpath services retain exact types sets and lifecycle order`() = runTest {
        val graph = services()
        val events = graph.resolve<Events>()
        try {
            assertSame(graph.resolve<Settings>(), graph.resolve<Client>().settings)
            assertEquals(setOf("json", "xml"), graph.resolve<Codecs>().values.map { it.name }.toSet())
            assertEquals(listOf("string"), graph.resolve<GenericClient>().strings)
            assertEquals(listOf(42), graph.resolve<GenericClient>().numbers)
            graph.start()
            graph.start()
        } finally { graph.stop() }
        assertEquals(listOf("dependency.start", "dependent.start", "dependent.stop", "dependency.stop"), events.values)
    }
}
