package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpecificationExtensionsTest {
    @Test
    fun `should create specification extensions with valid keys`() {
        // Given valid extension keys starting with x-
        val extensions = specificationExtensions(
            "x-custom" to JsonPrimitive("value"),
            "x-internal-id" to JsonPrimitive(123),
            "x-feature-flag" to JsonPrimitive(true),
        )

        // Then
        assertEquals(
            mapOf(
                "x-custom" to JsonPrimitive("value"),
                "x-internal-id" to JsonPrimitive(123),
                "x-feature-flag" to JsonPrimitive(true)
            ),
            extensions
        )
    }

    @Test
    fun `should throw exception for invalid extension keys`() {
        // When creating extensions with invalid key (not starting with x-)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            specificationExtensions(
                "invalid-key" to JsonPrimitive("value"),
            )
        }

        // Then
        assertTrue(exception.message!!.contains("must start with 'x-'"))
        assertTrue(exception.message!!.contains("invalid-key"))
    }

    @Test
    fun `should create empty specification extensions`() {
        // When creating empty extensions
        val extensions = emptySpecificationExtensions()

        // Then
        assertTrue(extensions.isEmpty())
    }

    @Test
    fun `should allow creating extensions with no parameters`() {
        // When creating extensions with no parameters
        val extensions = specificationExtensions()

        // Then
        assertTrue(extensions.isEmpty())
    }
}
