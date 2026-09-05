package io.heapy.komok.tech.runtime.ksp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ServiceResolutionDiagnosticsTest {
    @Test
    fun `missing binding message includes dependency chain and whole generic type key`() {
        val message = missingServiceBindingMessage(
            moduleFqn = "example.AppServices",
            providerName = "consumer",
            providerSignatureDescriptor = "(kotlin.collections.List<kotlin.String>)",
            dependencyName = "items",
            dependencyTypeKey = "kotlin.collections.List<kotlin.Int>",
        )

        assertAll(
            {
                assertEquals(
                    "No @Service binding found for dependency `items: kotlin.collections.List<kotlin.Int>`. " +
                        "Dependency chain: example.AppServices.consumer(kotlin.collections.List<kotlin.String>) " +
                        "-> items:kotlin.collections.List<kotlin.Int>.",
                    message,
                )
            },
            { assertTrue(message.contains("List<kotlin.Int>")) },
            { assertTrue(message.contains("List<kotlin.String>")) },
        )
    }

    @Test
    fun `duplicate binding message includes both candidate addresses`() {
        val message = duplicateServiceBindingMessage(
            moduleFqn = "example.AppServices",
            providerName = "consumer",
            providerSignatureDescriptor = "(example.Clock)",
            dependencyName = "clock",
            dependencyTypeKey = "example.Clock",
            candidateAddresses = listOf(
                "example.AppServices.systemClock",
                "example.AppServices.testClock",
            ),
        )

        assertEquals(
            "Multiple @Service bindings found for dependency `clock: example.Clock`. " +
                "Dependency chain: example.AppServices.consumer(example.Clock) -> clock:example.Clock. " +
                "Candidates: example.AppServices.systemClock, example.AppServices.testClock.",
            message,
        )
    }
}
