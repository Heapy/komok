package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HeaderTest {

    @Test
    fun `should serialize Header with schema`() {
        val header = Header(
            description = "The number of allowed requests in the current period",
            schema = Schema(buildJsonObject { put("type", "integer") })
        )
        val json = compactJson.encodeToString(header)

        val expected = """{"description":"The number of allowed requests in the current period","schema":{"type":"integer"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Header with required and deprecated`() {
        val header = Header(
            description = "API key",
            required = true,
            deprecated = false,
            schema = Schema(buildJsonObject { put("type", "string") })
        )
        val json = compactJson.encodeToString(header)

        val expected = """{"description":"API key","required":true,"schema":{"type":"string"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Header with content`() {
        val header = Header(
            description = "Custom header",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            )
        )
        val json = compactJson.encodeToString(header)

        val expected = """{"description":"Custom header","content":{"application/json":{"schema":{"type":"object"}}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Header with style and explode`() {
        val header = Header(
            schema = Schema(buildJsonObject { put("type", "array") }),
            style = "simple",
            explode = false
        )
        val json = compactJson.encodeToString(header)

        val expected = """{"schema":{"type":"array"},"style":"simple","explode":false}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Header with example`() {
        val header = Header(
            description = "Page number",
            schema = Schema(buildJsonObject { put("type", "integer") }),
            example = JsonPrimitive(1)
        )
        val json = compactJson.encodeToString(header)

        val expected = """{"description":"Page number","schema":{"type":"integer"},"example":1}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Header with examples`() {
        val header = Header(
            description = "Token",
            schema = Schema(buildJsonObject { put("type", "string") }),
            examples = mapOf(
                "example1" to Example(
                    summary = "First token",
                    value = JsonPrimitive("abc123")
                ),
                "example2" to Example(
                    summary = "Second token",
                    value = JsonPrimitive("xyz789")
                )
            )
        )
        val json = compactJson.encodeToString(header)

        val expected = """{"description":"Token","schema":{"type":"string"},"examples":{"example1":{"summary":"First token","value":"abc123"},"example2":{"summary":"Second token","value":"xyz789"}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should reject Header without schema or content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Header(description = "Invalid header")
        }

        assertEquals(
            "Header must have exactly one of 'schema' or 'content' specified",
            exception.message
        )
    }

    @Test
    fun `should reject Header with both schema and content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Header(
                description = "Invalid header",
                schema = Schema(buildJsonObject { put("type", "string") }),
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "string") })
                    )
                )
            )
        }

        assertEquals(
            "Header must have exactly one of 'schema' or 'content' specified",
            exception.message
        )
    }

    @Test
    fun `should reject Header with both example and examples`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Header(
                schema = Schema(buildJsonObject { put("type", "string") }),
                example = JsonPrimitive("test"),
                examples = mapOf("ex1" to Example(value = JsonPrimitive("test")))
            )
        }

        assertEquals(
            "Header 'example' and 'examples' are mutually exclusive",
            exception.message
        )
    }

    @Test
    fun `should deserialize Header`() {
        val json = """{"description":"Rate limit","required":true,"schema":{"type":"integer"}}"""
        val header = compactJson.decodeFromString<Header>(json)

        assertEquals(
            Header(
                description = "Rate limit",
                required = true,
                schema = Schema(buildJsonObject { put("type", "integer") })
            ),
            header
        )
    }

    @Test
    fun `should round-trip Header with schema`() {
        val header = Header(
            description = "X-Request-ID",
            required = true,
            schema = Schema(buildJsonObject {
                put("type", "string")
                put("format", "uuid")
            })
        )

        TestHelpers.testRoundTripWithoutValidation(Header.serializer(), header)
    }

    @Test
    fun `should round-trip Header with content`() {
        val header = Header(
            description = "Complex header",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("code", buildJsonObject { put("type", "integer") })
                            put("message", buildJsonObject { put("type", "string") })
                        })
                    })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Header.serializer(), header)
    }
}
