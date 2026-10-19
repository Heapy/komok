package io.heapy.komok.tech.runtime.services

import io.heapy.komok.tech.runtime.*
import io.heapy.komok.tech.runtime.EdgeDescriptor
import io.heapy.komok.tech.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ServiceGraphTest {
    /** #20 — @Service functions/classes are realized through the synthetic module. */
    @Test
    fun `service function and class providers resolve parameters through the synthetic module`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val service = graph.resolve<ClassBackedServiceProvider>()
        val serviceAgain = graph.resolve<ClassBackedServiceProvider>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("class:root", service.value()) },
            { assertSame(service, serviceAgain) },
            {
                assertTrue(
                    edges.any {
                        it.from.endsWith(".Services.classBackedServiceProvider") &&
                            it.to.endsWith(".Services.functionRepository")
                    },
                )
            },
            {
                assertTrue(
                    edges.any {
                        it.from.endsWith(".Services.functionRepository") &&
                            it.to.endsWith(".Services.serviceConfig")
                    },
                )
            },
            {
                assertEquals(
                    "(io.heapy.komok.tech.runtime.services.FunctionRepository)",
                    graph.binding<ClassBackedServiceProvider>().id.signature.valueParamsDescriptor,
                )
            },
        )
        graph.stop()
    }

    /** #20 — synthetic providers are replaceable in the generated ForkScope. */
    @Test
    fun `service fork replacement recomputes dependent synthetic providers`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val rootService = graph.resolve<ClassBackedServiceProvider>()
        val fork = graph.fork {
                replace(binding<ServiceConfig>(), ServiceConfig("fork"))
        }
        val forkService = fork.resolve<ClassBackedServiceProvider>()

        assertAll(
            { assertEquals("class:root", rootService.value()) },
            { assertEquals("class:fork", forkService.value()) },
            { assertNotSame(rootService, forkService) },
        )
        fork.stop()
        graph.stop()
    }

    /** #20 — @Service bind/value contributes under an interface for exact-match resolution. */
    @Test
    fun `service bind and value annotations expose providers under interface types`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val client = graph.resolve<PaymentClient>()
        val gateway = graph.resolve<PaymentGateway>()
        val settings = graph.resolve<PaymentSettings>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("charged:live", client.checkout()) },
            { assertEquals("charged", gateway.charge()) },
            { assertEquals("live", settings.mode) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.runtime.services.PaymentGateway, " +
                        "io.heapy.komok.tech.runtime.services.PaymentSettings)",
                    graph.binding<PaymentClient>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<PaymentClient>().id.display(),
                            graph.binding<PaymentGateway>().id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<PaymentClient>().id.display(),
                            graph.binding<PaymentSettings>().id.display(),
                        ),
                    ),
                )
            },
        )
        graph.stop()
    }

    /** #21 — synthetic service dependencies can resolve from a dependency jar's binding index. */
    @Test
    fun `service dependency resolves from classpath binding index`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val consumer = graph.resolve<IndexedTimeZoneConsumer>()
        val indexedProviderValue = graph.resolve<IndexedTimeZone>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("UTC", consumer.zoneId()) },
            { assertEquals("UTC", indexedProviderValue.id) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.time.IndexedTimeZone)",
                    graph.binding<IndexedTimeZoneConsumer>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<IndexedTimeZoneConsumer>().id.display(),
                            graph.binding<IndexedTimeZone>().id.display(),
                        ),
                    ),
                )
            },
        )
        graph.stop()
    }

    /** #27 — @ProvidersFrom imports provider-shaped functions into the binding index. */
    @Test
    fun `providers-from imports dependency object providers into the synthetic graph and index`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val consumer = graph.resolve<ProvidersFromTimeZoneConsumer>()
        val providerValue = graph.resolve<ProvidersFromTimeZone>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("providers-from", consumer.zoneId()) },
            { assertEquals("providers-from", providerValue.id) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.time.ProvidersFromTimeZone)",
                    graph.binding<ProvidersFromTimeZoneConsumer>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<ProvidersFromTimeZoneConsumer>().id.display(),
                            graph.binding<ProvidersFromTimeZone>().id.display(),
                        ),
                    ),
                )
            },
        )
        graph.stop()
    }

    /** #22 — exact type resolution includes whole generic type arguments. */
    @Test
    fun `service exact type resolution distinguishes whole generic type keys`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val stringConsumer = graph.resolve<GenericStringItemsConsumer>()
        val intConsumer = graph.resolve<GenericIntItemsConsumer>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("alpha,beta", stringConsumer.joined()) },
            { assertEquals(30, intConsumer.total()) },
            {
                assertEquals(
                    "(kotlin.collections.List<kotlin.String>)",
                    graph.binding<GenericStringItemsConsumer>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertEquals(
                    "(kotlin.collections.List<kotlin.Int>)",
                    graph.binding<GenericIntItemsConsumer>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<GenericStringItemsConsumer>().id.display(),
                            graph.binding<List<String>>().id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<GenericIntItemsConsumer>().id.display(),
                            graph.binding<List<Int>>().id.display(),
                        ),
                    ),
                )
            },
        )
        graph.stop()
    }

    /** #20 — @IntoSet service functions aggregate into Set<T> and remain replaceable. */
    @Test
    fun `service into-set contributions aggregate and preserve fork replacement semantics`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val registry = graph.resolve<CodecRegistry>()
        val aggregate = graph.resolve<Set<Codec>>()
        val forkWithContributionReplacement = graph.fork {
                replace(binding(::jsonCodec), NamedCodec("yaml"))
        }
        val forkRegistry = forkWithContributionReplacement.resolve<CodecRegistry>()
        val forkWithSetReplacement = graph.fork {
                replace(binding<Set<Codec>>(), setOf(NamedCodec("only")))
        }
        val setReplacementRegistry = forkWithSetReplacement.resolve<CodecRegistry>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals(listOf("json", "xml"), registry.names()) },
            { assertEquals(listOf("json", "xml"), aggregate.map { it.name }) },
            { assertEquals(listOf("yaml", "xml"), forkRegistry.names()) },
            { assertEquals(listOf("only"), setReplacementRegistry.names()) },
            { assertNotSame(registry, forkRegistry) },
            { assertNotSame(registry, setReplacementRegistry) },
            {
                assertEquals(
                    "(kotlin.collections.Set<io.heapy.komok.tech.runtime.services.Codec>)",
                    graph.binding<CodecRegistry>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<CodecRegistry>().id.display(),
                            graph.binding<Set<Codec>>().id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<Set<Codec>>().id.display(),
                            binding(::jsonCodec).id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<Set<Codec>>().id.display(),
                            binding(::xmlCodec).id.display(),
                        ),
                    ),
                )
            },
        )
        forkWithSetReplacement.stop()
        forkWithContributionReplacement.stop()
        graph.stop()
    }

    /** #24 + #25 — classpath @IntoSet contributions close in the downstream root aggregate. */
    @Test
    fun `set aggregation consumes classpath contributions and preserves fork replacement semantics`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val registry = graph.resolve<TimeCodecRegistry>()
        val aggregate = graph.resolve<Set<TimeCodec>>()
        val replacement = object : TimeCodec {
            override val name: String = "override"
        }
        val fork = graph.fork {
                replace(binding(::zoneTimeCodec), replacement)
        }
        val forkRegistry = fork.resolve<TimeCodecRegistry>()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals(listOf("zone"), registry.names()) },
            { assertEquals(listOf("zone"), aggregate.map { it.name }) },
            { assertEquals(listOf("override"), forkRegistry.names()) },
            { assertNotSame(registry, forkRegistry) },
            {
                assertEquals(
                    "(kotlin.collections.Set<io.heapy.komok.tech.time.TimeCodec>)",
                    graph.binding<TimeCodecRegistry>().id.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<TimeCodecRegistry>().id.display(),
                            graph.binding<Set<TimeCodec>>().id.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            graph.binding<Set<TimeCodec>>().id.display(),
                            binding(::zoneTimeCodec).id.display(),
                        ),
                    ),
                )
            },
        )
        fork.stop()
        graph.stop()
    }

    /** #8 + #20 — replacement bodies read other synthetic providers and record edges. */
    @Test
    fun `service provider replacement body reads forked dependencies and records edges`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val fork = graph.fork {
                replace(binding<ServiceConfig>(), ServiceConfig("fork"))
                replaceWith(binding<FunctionRepository>()) {
                    FunctionRepository(config = provider<ServiceConfig>(), source = "replacement")
                }
        }
        val repository = fork.resolve<FunctionRepository>()

        assertAll(
            { assertEquals(FunctionRepository(ServiceConfig("fork"), "replacement"), repository) },
            {
                assertTrue(
                    graph.graphPayload().edges.contains(
                        EdgeDescriptor(
                            graph.binding<FunctionRepository>().id.display(),
                            graph.binding<ServiceConfig>().id.display(),
                        ),
                    ),
                )
            },
        )
        fork.stop()
        graph.stop()
    }

    /** #28 — @Service-class lifecycle hooks run in synthetic-module dependency order. */
    @Test
    fun `service lifecycle hooks run deps-first on startup and reverse on shutdown`() = runTest {
        val graph = services { dispatcher = Dispatchers.Default }

        val events = graph.resolve<LifecycleEvents>().entries
        graph.start()
        graph.stop()

        assertEquals(
            listOf(
                "dependency.start",
                "dependent.start",
                "dependent.stop",
                "dependency.stop",
            ),
            events,
        )
    }
}
