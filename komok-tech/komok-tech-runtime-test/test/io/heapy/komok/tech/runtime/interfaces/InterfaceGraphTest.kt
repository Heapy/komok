package io.heapy.komok.tech.runtime.interfaces

import io.heapy.komok.tech.runtime.*
import io.heapy.komok.tech.runtime.EdgeDescriptor
import io.heapy.komok.tech.runtime.services.serviceInterfaceCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class InterfaceGraphTest {
    /** Interface-module no-arg providers are generated as leaf bindings. */
    @Test
    fun `interface module leaf provider is available through the generated graph`() = runTest {
        val graph = graph<InterfaceLeafModule> { dispatcher = Dispatchers.Default }

        val answer = graph.read { module<InterfaceLeafModule>().answer() }
        val payload = graph.graphPayload()

        assertAll(
            { assertEquals(42, answer) },
            {
                assertTrue(
                    payload.modules.any {
                        it.fqn.endsWith(".InterfaceLeafModule") &&
                            it.bindings.any { binding -> binding.endsWith(".InterfaceLeafModule.answer") }
                    },
                )
            },
        )
        graph.stop()
    }

    /** Interface-module value parameters are resolved as dependencies inside the binding body. */
    @Test
    fun `interface module param-wired provider resolves dependencies and records edges`() = runTest {
        val graph = graph<InterfaceParamModule> { dispatcher = Dispatchers.Default }

        val endpoint = graph[InterfaceParamModule.Bindings.endpoint]
        val payload = graph.graphPayload()

        assertAll(
            { assertEquals(InterfaceEndpoint("localhost:8080"), endpoint) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.runtime.interfaces.InterfaceHost, " +
                        "io.heapy.komok.tech.runtime.interfaces.InterfacePort)",
                    InterfaceParamModule.Bindings.endpoint.id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceParamModule.Bindings.endpoint.id.display(),
                            InterfaceParamModule.Bindings.host.id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceParamModule.Bindings.endpoint.id.display(),
                            InterfaceParamModule.Bindings.port.id.display(),
                        ),
                    ),
                )
            },
        )
        graph.stop()
    }

    /** #24 — module @IntoSet providers aggregate into an ordered Set<T> binding. */
    @Test
    fun `interface module into-set contributions aggregate and preserve fork replacement semantics`() = runTest {
        val graph = graph<InterfaceSetModule> { dispatcher = Dispatchers.Default }

        val registry = graph[InterfaceSetModule.Bindings.interfaceCodecRegistry]
        val aggregate = graph.resolve<Set<InterfaceCodec>>()
        val contributionFork = graph.fork {
            replace(InterfaceSetModule.Bindings.jsonInterfaceCodec, InterfaceNamedCodec("yaml"))
        }
        val contributionForkRegistry = contributionFork[InterfaceSetModule.Bindings.interfaceCodecRegistry]
        val serviceContributionFork = graph.fork {
            replace(binding(::serviceInterfaceCodec), InterfaceNamedCodec("service-fork"))
        }
        val serviceContributionForkRegistry = serviceContributionFork[InterfaceSetModule.Bindings.interfaceCodecRegistry]
        val setFork = graph.fork {
            replace(binding<Set<InterfaceCodec>>(), setOf(InterfaceNamedCodec("only")))
        }
        val setForkRegistry = setFork[InterfaceSetModule.Bindings.interfaceCodecRegistry]
        val payload = graph.graphPayload()

        assertAll(
            { assertEquals(listOf("json", "xml", "service"), registry.names()) },
            { assertEquals(listOf("json", "xml", "service"), aggregate.map { it.name }) },
            { assertEquals(listOf("yaml", "xml", "service"), contributionForkRegistry.names()) },
            { assertEquals(listOf("json", "xml", "service-fork"), serviceContributionForkRegistry.names()) },
            { assertEquals(listOf("only"), setForkRegistry.names()) },
            { assertNotSame(registry, contributionForkRegistry) },
            { assertNotSame(registry, serviceContributionForkRegistry) },
            { assertNotSame(registry, setForkRegistry) },
            {
                assertEquals(
                    "(kotlin.collections.Set<io.heapy.komok.tech.runtime.interfaces.InterfaceCodec>)",
                    InterfaceSetModule.Bindings.interfaceCodecRegistry.id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    payload.modules.single { it.fqn == "komok.sets" }
                        .bindings.contains(graph.binding<Set<InterfaceCodec>>().id.display()),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceSetModule.Bindings.interfaceCodecRegistry.id.display(),
                            graph.binding<Set<InterfaceCodec>>().id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.modules.single { it.fqn == "io.heapy.komok.tech.runtime.services.Services" }
                        .bindings.any { it.endsWith(".Services.serviceInterfaceCodec") },
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            graph.binding<Set<InterfaceCodec>>().id.display(),
                            InterfaceSetModule.Bindings.jsonInterfaceCodec.id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            graph.binding<Set<InterfaceCodec>>().id.display(),
                            InterfaceSetModule.Bindings.xmlInterfaceCodec.id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            graph.binding<Set<InterfaceCodec>>().id.display(),
                            binding(::serviceInterfaceCodec).id.display(),
                        ),
                    ),
                )
            },
        )
        setFork.stop()
        serviceContributionFork.stop()
        contributionFork.stop()
        graph.stop()
    }
}
