package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParameterTest {

    // Basic Parameter Tests

    @Test
    fun `should serialize minimal Parameter with query location`() {
        val parameter = Parameter(
            name = "limit",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "integer") })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"limit","in":"query","schema":{"type":"integer"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with all basic properties`() {
        val parameter = Parameter(
            name = "userId",
            location = ParameterLocation.PATH,
            description = "The user ID",
            required = true,
            deprecated = false,
            schema = Schema(buildJsonObject {
                put("type", "string")
                put("format", "uuid")
            })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"userId","in":"path","description":"The user ID","required":true,"schema":{"type":"string","format":"uuid"}}"""
        assertEquals(expected, json)
    }

    // Parameter Location Tests

    @Test
    fun `should serialize Parameter with query location`() {
        val parameter = Parameter(
            name = "search",
            location = ParameterLocation.QUERY,
            description = "Search query",
            schema = Schema(buildJsonObject { put("type", "string") })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"search","in":"query","description":"Search query","schema":{"type":"string"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with querystring location`() {
        val parameter = Parameter(
            name = "filter",
            location = ParameterLocation.QUERYSTRING,
            description = "Complex filter",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            )
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"filter","in":"querystring","description":"Complex filter","content":{"application/json":{"schema":{"type":"object"}}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with header location`() {
        val parameter = Parameter(
            name = "X-Request-ID",
            location = ParameterLocation.HEADER,
            description = "Request ID for tracing",
            schema = Schema(buildJsonObject {
                put("type", "string")
                put("format", "uuid")
            })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"X-Request-ID","in":"header","description":"Request ID for tracing","schema":{"type":"string","format":"uuid"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with path location`() {
        val parameter = Parameter(
            name = "id",
            location = ParameterLocation.PATH,
            description = "Resource ID",
            required = true,
            schema = Schema(buildJsonObject { put("type", "integer") })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"id","in":"path","description":"Resource ID","required":true,"schema":{"type":"integer"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with cookie location`() {
        val parameter = Parameter(
            name = "session",
            location = ParameterLocation.COOKIE,
            description = "Session cookie",
            schema = Schema(buildJsonObject { put("type", "string") })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"session","in":"cookie","description":"Session cookie","schema":{"type":"string"}}"""
        assertEquals(expected, json)
    }

    // Parameter Style Tests

    @Test
    fun `should serialize Parameter with matrix style`() {
        val parameter = Parameter(
            name = "id",
            location = ParameterLocation.PATH,
            required = true,
            schema = Schema(buildJsonObject { put("type", "string") }),
            style = ParameterStyle.MATRIX
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"id","in":"path","required":true,"schema":{"type":"string"},"style":"matrix"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with label style`() {
        val parameter = Parameter(
            name = "id",
            location = ParameterLocation.PATH,
            required = true,
            schema = Schema(buildJsonObject { put("type", "string") }),
            style = ParameterStyle.LABEL
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"id","in":"path","required":true,"schema":{"type":"string"},"style":"label"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with simple style`() {
        val parameter = Parameter(
            name = "id",
            location = ParameterLocation.PATH,
            required = true,
            schema = Schema(buildJsonObject { put("type", "string") }),
            style = ParameterStyle.SIMPLE
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"id","in":"path","required":true,"schema":{"type":"string"},"style":"simple"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with form style`() {
        val parameter = Parameter(
            name = "tags",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "array") }),
            style = ParameterStyle.FORM
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"tags","in":"query","schema":{"type":"array"},"style":"form"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with spaceDelimited style`() {
        val parameter = Parameter(
            name = "tags",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "array") }),
            style = ParameterStyle.SPACE_DELIMITED
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"tags","in":"query","schema":{"type":"array"},"style":"spaceDelimited"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with pipeDelimited style`() {
        val parameter = Parameter(
            name = "tags",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "array") }),
            style = ParameterStyle.PIPE_DELIMITED
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"tags","in":"query","schema":{"type":"array"},"style":"pipeDelimited"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with deepObject style`() {
        val parameter = Parameter(
            name = "filter",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "object") }),
            style = ParameterStyle.DEEP_OBJECT
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"filter","in":"query","schema":{"type":"object"},"style":"deepObject"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with cookie style`() {
        val parameter = Parameter(
            name = "session",
            location = ParameterLocation.COOKIE,
            schema = Schema(buildJsonObject { put("type", "string") }),
            style = ParameterStyle.COOKIE
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"session","in":"cookie","schema":{"type":"string"},"style":"cookie"}"""
        assertEquals(expected, json)
    }

    // Explode and AllowReserved Tests

    @Test
    fun `should serialize Parameter with explode`() {
        val parameter = Parameter(
            name = "tags",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "array") }),
            style = ParameterStyle.FORM,
            explode = true
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"tags","in":"query","schema":{"type":"array"},"style":"form","explode":true}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with allowReserved`() {
        val parameter = Parameter(
            name = "path",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "string") }),
            allowReserved = true
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"path","in":"query","schema":{"type":"string"},"allowReserved":true}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with allowEmptyValue`() {
        val parameter = Parameter(
            name = "filter",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "string") }),
            allowEmptyValue = true
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"filter","in":"query","schema":{"type":"string"},"allowEmptyValue":true}"""
        assertEquals(expected, json)
    }

    // Content Tests

    @Test
    fun `should serialize Parameter with content`() {
        val parameter = Parameter(
            name = "coordinates",
            location = ParameterLocation.QUERY,
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("required", buildJsonObject {
                            put("lat", "number")
                            put("lng", "number")
                        })
                    })
                )
            )
        )
        val json = compactJson.encodeToString(parameter)

        // Note: required is using buildJsonObject which creates an object, not an array
        // This is intentional for this test - the actual validation happens via JSON schema
        val expected = """{"name":"coordinates","in":"query","content":{"application/json":{"schema":{"type":"object","required":{"lat":"number","lng":"number"}}}}}"""
        assertEquals(expected, json)
    }

    // Example and Examples Tests

    @Test
    fun `should serialize Parameter with example`() {
        val parameter = Parameter(
            name = "limit",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "integer") }),
            example = JsonPrimitive(10)
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"limit","in":"query","schema":{"type":"integer"},"example":10}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with examples`() {
        val parameter = Parameter(
            name = "format",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "string") }),
            examples = mapOf(
                "json" to Example(
                    summary = "JSON format",
                    value = JsonPrimitive("json")
                ),
                "xml" to Example(
                    summary = "XML format",
                    value = JsonPrimitive("xml")
                )
            )
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"format","in":"query","schema":{"type":"string"},"examples":{"json":{"summary":"JSON format","value":"json"},"xml":{"summary":"XML format","value":"xml"}}}"""
        assertEquals(expected, json)
    }

    // Validation Tests - Mutual Exclusivity

    @Test
    fun `should reject Parameter without schema or content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "invalid",
                location = ParameterLocation.QUERY
            )
        }

        assertEquals(
            "Parameter must have exactly one of 'schema' or 'content' specified",
            exception.message
        )
    }

    @Test
    fun `should reject Parameter with both schema and content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "invalid",
                location = ParameterLocation.QUERY,
                schema = Schema(buildJsonObject { put("type", "string") }),
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "string") })
                    )
                )
            )
        }

        assertEquals(
            "Parameter must have exactly one of 'schema' or 'content' specified",
            exception.message
        )
    }

    @Test
    fun `should reject Parameter with both example and examples`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "invalid",
                location = ParameterLocation.QUERY,
                schema = Schema(buildJsonObject { put("type", "string") }),
                example = JsonPrimitive("test"),
                examples = mapOf("ex1" to Example(value = JsonPrimitive("test")))
            )
        }

        assertEquals(
            "Parameter 'example' and 'examples' are mutually exclusive",
            exception.message
        )
    }

    // Validation Tests - Location-specific Constraints

    @Test
    fun `should reject querystring Parameter with schema`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "filter",
                location = ParameterLocation.QUERYSTRING,
                schema = Schema(buildJsonObject { put("type", "object") })
            )
        }

        assertEquals(
            "Parameter with location 'querystring' must use 'content' (not 'schema')",
            exception.message
        )
    }

    @Test
    fun `should reject path Parameter without required`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "id",
                location = ParameterLocation.PATH,
                required = false,
                schema = Schema(buildJsonObject { put("type", "string") })
            )
        }

        assertEquals(
            "Parameter with location 'path' must have 'required' set to true",
            exception.message
        )
    }

    @Test
    fun `should reject Parameter with content containing multiple media types`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "data",
                location = ParameterLocation.QUERY,
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    ),
                    "application/xml" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                )
            )
        }

        assertEquals(
            "Parameter 'content' must have exactly one media type entry",
            exception.message
        )
    }

    @Test
    fun `should reject Parameter with style but no schema`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "data",
                location = ParameterLocation.QUERYSTRING,
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                ),
                style = ParameterStyle.FORM
            )
        }

        assertEquals(
            "Parameter properties 'style', 'explode', and 'allowReserved' can only be used with 'schema'",
            exception.message
        )
    }

    @Test
    fun `should reject Parameter with explode but no schema`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "data",
                location = ParameterLocation.QUERYSTRING,
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                ),
                explode = true
            )
        }

        assertEquals(
            "Parameter properties 'style', 'explode', and 'allowReserved' can only be used with 'schema'",
            exception.message
        )
    }

    @Test
    fun `should reject Parameter with allowReserved but no schema`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Parameter(
                name = "data",
                location = ParameterLocation.QUERYSTRING,
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                ),
                allowReserved = true
            )
        }

        assertEquals(
            "Parameter properties 'style', 'explode', and 'allowReserved' can only be used with 'schema'",
            exception.message
        )
    }

    @Test
    fun `should reject allowEmptyValue for non-query parameters`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            Parameter(
                name = "id",
                location = ParameterLocation.PATH,
                required = true,
                schema = Schema(buildJsonObject { put("type", "string") }),
                allowEmptyValue = true
            )
        }

        assertEquals(
            "Parameter property 'allowEmptyValue' can only be used with location 'query'",
            exception.message
        )
    }

    // Deserialization Tests

    @Test
    fun `should deserialize Parameter with query location`() {
        val json = """{"name":"limit","in":"query","schema":{"type":"integer"},"example":20}"""
        val parameter = compactJson.decodeFromString<Parameter>(json)

        assertEquals("limit", parameter.name)
        assertEquals(ParameterLocation.QUERY, parameter.location)
        assertEquals(Schema(buildJsonObject { put("type", "integer") }), parameter.schema)
        assertEquals(JsonPrimitive(20), parameter.example)
    }

    @Test
    fun `should deserialize Parameter with path location`() {
        val json = """{"name":"userId","in":"path","required":true,"schema":{"type":"string","format":"uuid"}}"""
        val parameter = compactJson.decodeFromString<Parameter>(json)

        assertEquals("userId", parameter.name)
        assertEquals(ParameterLocation.PATH, parameter.location)
        assertEquals(true, parameter.required)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip Parameter with schema`() {
        val parameter = Parameter(
            name = "offset",
            location = ParameterLocation.QUERY,
            description = "Pagination offset",
            required = false,
            deprecated = false,
            schema = Schema(buildJsonObject {
                put("type", "integer")
                put("minimum", 0)
                put("default", 0)
            }),
            style = ParameterStyle.FORM,
            example = JsonPrimitive(10)
        )

        TestHelpers.testRoundTripWithoutValidation(Parameter.serializer(), parameter)
    }

    @Test
    fun `should round-trip Parameter with content`() {
        val parameter = Parameter(
            name = "filter",
            location = ParameterLocation.QUERYSTRING,
            description = "Complex filter object",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                            put("age", buildJsonObject { put("type", "integer") })
                        })
                    })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Parameter.serializer(), parameter)
    }

    @Test
    fun `should round-trip path Parameter`() {
        val parameter = Parameter(
            name = "petId",
            location = ParameterLocation.PATH,
            description = "ID of pet to return",
            required = true,
            schema = Schema(buildJsonObject {
                put("type", "integer")
                put("format", "int64")
            })
        )

        TestHelpers.testRoundTripWithoutValidation(Parameter.serializer(), parameter)
    }

    @Test
    fun `should serialize Parameter with deprecated flag`() {
        val parameter = Parameter(
            name = "oldParam",
            location = ParameterLocation.QUERY,
            deprecated = true,
            schema = Schema(buildJsonObject { put("type", "string") })
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"oldParam","in":"query","deprecated":true,"schema":{"type":"string"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Parameter with specification extensions`() {
        val parameter = Parameter(
            name = "customParam",
            location = ParameterLocation.HEADER,
            schema = Schema(buildJsonObject { put("type", "string") }),
            extensions = mapOf("x-internal-id" to JsonPrimitive("param-123"))
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"customParam","in":"header","schema":{"type":"string"},"extensions":{"x-internal-id":"param-123"}}"""
        assertEquals(expected, json)
    }

    // Complex Real-world Examples

    @Test
    fun `should serialize complex query parameter with all style options`() {
        val parameter = Parameter(
            name = "tags",
            location = ParameterLocation.QUERY,
            description = "Tags to filter by",
            required = false,
            schema = Schema(buildJsonObject {
                put("type", "array")
                put("items", buildJsonObject { put("type", "string") })
            }),
            style = ParameterStyle.FORM,
            explode = true,
            example = buildJsonObject {
                put("tags", buildJsonObject {
                    put("0", "kotlin")
                    put("1", "openapi")
                })
            }
        )

        TestHelpers.testRoundTripWithoutValidation(Parameter.serializer(), parameter)
    }

    @Test
    fun `should serialize header parameter with simple style`() {
        val parameter = Parameter(
            name = "X-Rate-Limit",
            location = ParameterLocation.HEADER,
            description = "Calls per hour allowed by the user",
            schema = Schema(buildJsonObject { put("type", "integer") }),
            style = ParameterStyle.SIMPLE,
            example = JsonPrimitive(1000)
        )
        val json = compactJson.encodeToString(parameter)

        val expected = """{"name":"X-Rate-Limit","in":"header","description":"Calls per hour allowed by the user","schema":{"type":"integer"},"style":"simple","example":1000}"""
        assertEquals(expected, json)
    }
}
