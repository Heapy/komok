package io.heapy.komok.tech.runtime.interfaces

import io.heapy.komok.tech.runtime.EdgeDescriptor
import io.heapy.komok.tech.runtime.services.AppServicesKeys
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
        val graph = createInterfaceLeafModuleGraph { dispatcher = Dispatchers.Default }

        val answer = graph.modules.interfaceLeafModule.answer()
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
        val graph = createInterfaceParamModuleGraph { dispatcher = Dispatchers.Default }

        val endpoint = graph.endpoint()
        val payload = graph.graphPayload()

        assertAll(
            { assertEquals(InterfaceEndpoint("localhost:8080"), endpoint) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.runtime.interfaces.InterfaceHost, " +
                        "io.heapy.komok.tech.runtime.interfaces.InterfacePort)",
                    InterfaceParamModuleKeys.endpoint.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceParamModuleKeys.endpoint.display(),
                            InterfaceParamModuleKeys.host.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceParamModuleKeys.endpoint.display(),
                            InterfaceParamModuleKeys.port.display(),
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
        val graph = createInterfaceSetModuleGraph { dispatcher = Dispatchers.Default }

        val registry = graph.interfaceCodecRegistry()
        val aggregate = graph.modules.interfaceSetModule.interfaceCodecSet()
        val contributionFork = graph.fork {
            interfaceSetModule {
                replaceJsonInterfaceCodec(InterfaceNamedCodec("yaml"))
            }
        }
        val contributionForkRegistry = contributionFork.interfaceCodecRegistry()
        val serviceContributionFork = graph.fork {
            appServices {
                replaceServiceInterfaceCodec(InterfaceNamedCodec("service-fork"))
            }
        }
        val serviceContributionForkRegistry = serviceContributionFork.interfaceCodecRegistry()
        val setFork = graph.fork {
            interfaceSetModule {
                replaceInterfaceCodecSet(setOf(InterfaceNamedCodec("only")))
            }
        }
        val setForkRegistry = setFork.interfaceCodecRegistry()
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
                    InterfaceSetModuleKeys.interfaceCodecRegistry.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    payload.modules.single { it.fqn.endsWith(".InterfaceSetModule") }
                        .bindings.any { it.endsWith(".InterfaceSetModule.interfaceCodecSet") },
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceSetModuleKeys.interfaceCodecRegistry.display(),
                            InterfaceSetModuleKeys.interfaceCodecSet.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.modules.single { it.fqn.endsWith(".AppServices") }
                        .bindings.any { it.endsWith(".AppServices.serviceInterfaceCodec") },
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceSetModuleKeys.interfaceCodecSet.display(),
                            InterfaceSetModuleKeys.jsonInterfaceCodec.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceSetModuleKeys.interfaceCodecSet.display(),
                            InterfaceSetModuleKeys.xmlInterfaceCodec.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    payload.edges.contains(
                        EdgeDescriptor(
                            InterfaceSetModuleKeys.interfaceCodecSet.display(),
                            AppServicesKeys.serviceInterfaceCodec.display(),
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
