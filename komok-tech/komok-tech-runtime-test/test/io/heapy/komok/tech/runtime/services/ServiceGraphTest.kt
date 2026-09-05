package io.heapy.komok.tech.runtime.services

import io.heapy.komok.tech.runtime.fixtures.generatedKspMainSourceRoot
import io.heapy.komok.tech.runtime.EdgeDescriptor
import io.heapy.komok.tech.time.TimeCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.nio.file.Files

class ServiceGraphTest {
    /** #20 — @Service functions/classes are realized through the synthetic module. */
    @Test
    fun `service function and class providers resolve parameters through the synthetic module`() = runTest {
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val service = graph.classBackedServiceProvider()
        val serviceAgain = graph.modules.appServices.classBackedServiceProvider()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("class:root", service.value()) },
            { assertSame(service, serviceAgain) },
            {
                assertTrue(
                    edges.any {
                        it.from.endsWith(".AppServices.classBackedServiceProvider") &&
                            it.to.endsWith(".AppServices.functionRepository")
                    },
                )
            },
            {
                assertTrue(
                    edges.any {
                        it.from.endsWith(".AppServices.functionRepository") &&
                            it.to.endsWith(".AppServices.serviceConfig")
                    },
                )
            },
            {
                assertEquals(
                    "(io.heapy.komok.tech.runtime.services.FunctionRepository)",
                    AppServicesKeys.classBackedServiceProvider.signature.valueParamsDescriptor,
                )
            },
        )
        graph.stop()
    }

    /** #20 — synthetic providers are replaceable in the generated ForkScope. */
    @Test
    fun `service fork replacement recomputes dependent synthetic providers`() = runTest {
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val rootService = graph.classBackedServiceProvider()
        val fork = graph.fork {
            appServices {
                replaceServiceConfig(ServiceConfig("fork"))
            }
        }
        val forkService = fork.classBackedServiceProvider()

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
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val client = graph.paymentClient()
        val gateway = graph.modules.appServices.realPaymentGateway()
        val settings = graph.modules.appServices.paymentSettings()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("charged:live", client.checkout()) },
            { assertEquals("charged", gateway.charge()) },
            { assertEquals("live", settings.mode) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.runtime.services.PaymentGateway, " +
                        "io.heapy.komok.tech.runtime.services.PaymentSettings)",
                    AppServicesKeys.paymentClient.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.paymentClient.display(),
                            AppServicesKeys.realPaymentGateway.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.paymentClient.display(),
                            AppServicesKeys.paymentSettings.display(),
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
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val consumer = graph.indexedTimeZoneConsumer()
        val indexedProviderValue = graph.modules.appServices.indexedTimeZone()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("UTC", consumer.zoneId()) },
            { assertEquals("UTC", indexedProviderValue.id) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.time.IndexedTimeZone)",
                    AppServicesKeys.indexedTimeZoneConsumer.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.indexedTimeZoneConsumer.display(),
                            AppServicesKeys.indexedTimeZone.display(),
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
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val consumer = graph.providersFromTimeZoneConsumer()
        val providerValue = graph.modules.appServices.providersFromTimeZone()
        val edges = graph.graphPayload().edges
        val generatedIndex = Files.readString(generatedKspMainSourceRoot().resolve("komok/index/__KOMOK__Index__.kt"))

        assertAll(
            { assertEquals("providers-from", consumer.zoneId()) },
            { assertEquals("providers-from", providerValue.id) },
            {
                assertEquals(
                    "(io.heapy.komok.tech.time.ProvidersFromTimeZone)",
                    AppServicesKeys.providersFromTimeZoneConsumer.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.providersFromTimeZoneConsumer.display(),
                            AppServicesKeys.providersFromTimeZone.display(),
                        ),
                    ),
                )
            },
            { assertTrue(generatedIndex.contains("objectFunction|providersFromTimeZone"), generatedIndex) },
            { assertTrue(generatedIndex.contains("io.heapy.komok.tech.time.LegacyTimeProviders"), generatedIndex) },
        )
        graph.stop()
    }

    /** #22 — exact type resolution includes whole generic type arguments. */
    @Test
    fun `service exact type resolution distinguishes whole generic type keys`() = runTest {
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val stringConsumer = graph.genericStringItemsConsumer()
        val intConsumer = graph.genericIntItemsConsumer()
        val edges = graph.graphPayload().edges

        assertAll(
            { assertEquals("alpha,beta", stringConsumer.joined()) },
            { assertEquals(30, intConsumer.total()) },
            {
                assertEquals(
                    "(kotlin.collections.List<kotlin.String>)",
                    AppServicesKeys.genericStringItemsConsumer.signature.valueParamsDescriptor,
                )
            },
            {
                assertEquals(
                    "(kotlin.collections.List<kotlin.Int>)",
                    AppServicesKeys.genericIntItemsConsumer.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.genericStringItemsConsumer.display(),
                            AppServicesKeys.stringItems.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.genericIntItemsConsumer.display(),
                            AppServicesKeys.intItems.display(),
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
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val registry = graph.codecRegistry()
        val aggregate = graph.modules.appServices.codecSet()
        val forkWithContributionReplacement = graph.fork {
            appServices {
                replaceJsonCodec(NamedCodec("yaml"))
            }
        }
        val forkRegistry = forkWithContributionReplacement.codecRegistry()
        val forkWithSetReplacement = graph.fork {
            appServices {
                replaceCodecSet(setOf(NamedCodec("only")))
            }
        }
        val setReplacementRegistry = forkWithSetReplacement.codecRegistry()
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
                    AppServicesKeys.codecRegistry.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.codecRegistry.display(),
                            AppServicesKeys.codecSet.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.codecSet.display(),
                            AppServicesKeys.jsonCodec.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.codecSet.display(),
                            AppServicesKeys.xmlCodec.display(),
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
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val registry = graph.timeCodecRegistry()
        val aggregate = graph.modules.appServices.timeCodecSet()
        val replacement = object : TimeCodec {
            override val name: String = "override"
        }
        val fork = graph.fork {
            appServices {
                replaceZoneTimeCodec(replacement)
            }
        }
        val forkRegistry = fork.timeCodecRegistry()
        val edges = graph.graphPayload().edges
        val timeIndex = Files.readString(
            generatedKspMainSourceRoot("komok-tech-time").resolve("komok/index/__KOMOK__Index__.kt"),
        )
        val timeGeneratedSources = generatedKspMainSources("komok-tech-time")

        assertAll(
            { assertEquals(listOf("zone"), registry.names()) },
            { assertEquals(listOf("zone"), aggregate.map { it.name }) },
            { assertEquals(listOf("override"), forkRegistry.names()) },
            { assertNotSame(registry, forkRegistry) },
            {
                assertEquals(
                    "(kotlin.collections.Set<io.heapy.komok.tech.time.TimeCodec>)",
                    AppServicesKeys.timeCodecRegistry.signature.valueParamsDescriptor,
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.timeCodecRegistry.display(),
                            AppServicesKeys.timeCodecSet.display(),
                        ),
                    ),
                )
            },
            {
                assertTrue(
                    edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.timeCodecSet.display(),
                            AppServicesKeys.zoneTimeCodec.display(),
                        ),
                    ),
                )
            },
            { assertTrue(timeIndex.contains("setContributions = [\"function|zoneTimeCodec"), timeIndex) },
            { assertFalse(timeGeneratedSources.contains("timeCodecSet"), timeGeneratedSources) },
        )
        fork.stop()
        graph.stop()
    }

    /** #8 + #20 — replacement bodies read other synthetic providers and record edges. */
    @Test
    fun `service provider replacement body reads forked dependencies and records edges`() = runTest {
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        val fork = graph.fork {
            appServices {
                replaceServiceConfig(ServiceConfig("fork"))
                replaceFunctionRepositoryWith {
                    FunctionRepository(config = serviceConfig(), source = "replacement")
                }
            }
        }
        val repository = fork.functionRepository()

        assertAll(
            { assertEquals(FunctionRepository(ServiceConfig("fork"), "replacement"), repository) },
            {
                assertTrue(
                    graph.graphPayload().edges.contains(
                        EdgeDescriptor(
                            AppServicesKeys.functionRepository.display(),
                            AppServicesKeys.serviceConfig.display(),
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
        val graph = createAppServicesGraph { dispatcher = Dispatchers.Default }

        graph.start()
        graph.stop()
        val events = graph.modules.appServices.lifecycleEvents().entries

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

private fun generatedKspMainSources(
    projectName: String,
): String =
    Files.walk(generatedKspMainSourceRoot(projectName)).use { paths ->
        paths
            .filter { path -> Files.isRegularFile(path) }
            .filter { path -> path.fileName.toString().endsWith(".kt") }
            .map { path -> Files.readString(path) }
            .toList()
            .joinToString("\n")
    }
