package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReferenceableSerializerTest {

    private val headerSerializer = ReferenceableSerializer(Header.serializer())

    @Test
    fun `should serialize Direct Header as unwrapped object`() {
        val header = Header(
            description = "Rate limit header",
            schema = Schema(buildJsonObject { put("type", "integer") })
        )
        val referenceable: Referenceable<Header> = Direct(header)

        val json = compactJson.encodeToString(headerSerializer, referenceable)

        val expected = """{"description":"Rate limit header","schema":{"type":"integer"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Reference with only ref`() {
        val referenceable: Referenceable<Header> = Reference(
            ref = "#/components/headers/RateLimit"
        )

        val json = compactJson.encodeToString(headerSerializer, referenceable)

        val expected = """{"${"$"}ref":"#/components/headers/RateLimit"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Reference with summary and description`() {
        val referenceable: Referenceable<Header> = Reference(
            ref = "#/components/headers/RateLimit",
            summary = "Rate limit header",
            description = "The rate limit header for API requests"
        )

        val json = compactJson.encodeToString(headerSerializer, referenceable)

        val expected =
            $$"""{"$ref":"#/components/headers/RateLimit","summary":"Rate limit header","description":"The rate limit header for API requests"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should deserialize object without ref as Direct`() {
        val json = """{"description":"X-Request-ID","schema":{"type":"string"}}"""

        val result = compactJson.decodeFromString(headerSerializer, json)

        assertTrue(result is Direct<Header>)
        val direct = result as Direct<Header>
        assertEquals("X-Request-ID", direct.value.description)
    }

    @Test
    fun `should deserialize object with ref as Reference`() {
        val json = $$"""{"$ref":"#/components/headers/RequestId"}"""

        val result = compactJson.decodeFromString(headerSerializer, json)

        assertTrue(result is Reference)
        val reference = result as Reference
        assertEquals("#/components/headers/RequestId", reference.ref)
    }

    @Test
    fun `should deserialize Reference with summary and description`() {
        val json =
            $$"""{"$ref":"#/components/headers/Auth","summary":"Auth header","description":"Authentication header"}"""

        val result = compactJson.decodeFromString(headerSerializer, json)

        assertTrue(result is Reference)
        val reference = result as Reference
        assertEquals("#/components/headers/Auth", reference.ref)
        assertEquals("Auth header", reference.summary)
        assertEquals("Authentication header", reference.description)
    }

    @Test
    fun `should round-trip Direct Header`() {
        val header = Header(
            description = "Custom header",
            required = true,
            schema = Schema(buildJsonObject {
                put("type", "string")
                put("format", "uuid")
            })
        )
        val original: Referenceable<Header> = Direct(header)

        val json = compactJson.encodeToString(headerSerializer, original)
        val deserialized = compactJson.decodeFromString(headerSerializer, json)

        assertTrue(deserialized is Direct<Header>)
        assertEquals(header, (deserialized as Direct<Header>).value)
    }

    @Test
    fun `should round-trip Reference`() {
        val original: Referenceable<Header> = Reference(
            ref = "#/components/headers/Correlation",
            summary = "Correlation ID",
            description = "Unique identifier for request tracing"
        )

        val json = compactJson.encodeToString(headerSerializer, original)
        val deserialized = compactJson.decodeFromString(headerSerializer, json)

        assertEquals(original, deserialized)
    }

    @Test
    fun `should work with Map of Referenceable Headers`() {
        val mapSerializer = MapSerializer(
            String.serializer(),
            ReferenceableSerializer(Header.serializer())
        )

        val headers: Map<String, Referenceable<Header>> = mapOf(
            "X-Rate-Limit" to Direct(
                Header(
                    description = "Rate limit",
                    schema = Schema(buildJsonObject { put("type", "integer") })
                )
            ),
            "X-Request-ID" to Reference(ref = "#/components/headers/RequestId")
        )

        val json = compactJson.encodeToString(mapSerializer, headers)
        val deserialized = compactJson.decodeFromString(mapSerializer, json)

        assertEquals(headers.size, deserialized.size)
        assertTrue(deserialized["X-Rate-Limit"] is Direct<Header>)
        assertTrue(deserialized["X-Request-ID"] is Reference)
    }

    @Test
    fun `should serialize Example with Referenceable`() {
        val exampleSerializer = ReferenceableSerializer(Example.serializer())

        val directExample: Referenceable<Example> = Direct(
            Example(
                summary = "A pet example",
                description = "Example of a pet object"
            )
        )

        val json = compactJson.encodeToString(exampleSerializer, directExample)
        val expected = """{"summary":"A pet example","description":"Example of a pet object"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should deserialize Example Reference`() {
        val exampleSerializer = ReferenceableSerializer(Example.serializer())
        val json = $$"""{"$ref":"#/components/examples/PetExample"}"""

        val result = compactJson.decodeFromString(exampleSerializer, json)

        assertTrue(result is Reference)
        assertEquals("#/components/examples/PetExample", (result as Reference).ref)
    }
}
