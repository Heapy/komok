package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ResponseTest {

    // Basic Response Tests

    @Test
    fun `should serialize minimal Response`() {
        val response = Response(
            description = "Success"
        )
        val json = compactJson.encodeToString(response)

        val expected = """{"description":"Success"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Response with summary`() {
        val response = Response(
            summary = "Successful operation",
            description = "The request was successful"
        )
        val json = compactJson.encodeToString(response)

        val expected = """{"description":"The request was successful","summary":"Successful operation"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Response with content`() {
        val response = Response(
            description = "User data",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                )
            )
        )
        val json = compactJson.encodeToString(response)

        assertTrue(json.contains("User data"))
        assertTrue(json.contains("application/json"))
    }

    @Test
    fun `should serialize Response with headers`() {
        val response = Response(
            description = "Successful response with headers",
            headers = mapOf(
                "X-Rate-Limit" to Header(
                    description = "Rate limit remaining",
                    schema = Schema(buildJsonObject { put("type", "integer") })
                ),
                "X-Request-ID" to Header(
                    description = "Request identifier",
                    schema = Schema(buildJsonObject {
                        put("type", "string")
                        put("format", "uuid")
                    })
                )
            )
        )
        val json = compactJson.encodeToString(response)

        assertTrue(json.contains("X-Rate-Limit"))
        assertTrue(json.contains("X-Request-ID"))
        assertTrue(json.contains("Rate limit remaining"))
    }

    @Test
    fun `should serialize Response with multiple content types`() {
        val response = Response(
            description = "Pet data in multiple formats",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                ),
                "application/xml" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                ),
                "text/plain" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "string") })
                )
            )
        )
        val json = compactJson.encodeToString(response)

        assertTrue(json.contains("application/json"))
        assertTrue(json.contains("application/xml"))
        assertTrue(json.contains("text/plain"))
    }

    @Test
    fun `should serialize Response with examples`() {
        val response = Response(
            description = "User response",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") }),
                    examples = mapOf(
                        "admin" to Example(
                            summary = "Admin user",
                            value = buildJsonObject {
                                put("id", 1)
                                put("username", "admin")
                                put("role", "admin")
                            }
                        ),
                        "regular" to Example(
                            summary = "Regular user",
                            value = buildJsonObject {
                                put("id", 2)
                                put("username", "user")
                                put("role", "user")
                            }
                        )
                    )
                )
            )
        )
        val json = compactJson.encodeToString(response)

        assertTrue(json.contains("admin"))
        assertTrue(json.contains("regular"))
    }

    @Test
    fun `should serialize Response with specification extensions`() {
        val response = Response(
            description = "Custom response",
            extensions = mapOf("x-response-id" to JsonPrimitive("resp-123"))
        )
        val json = compactJson.encodeToString(response)

        val expected = """{"description":"Custom response","extensions":{"x-response-id":"resp-123"}}"""
        assertEquals(expected, json)
    }

    // Common HTTP Response Scenarios

    @Test
    fun `should serialize 200 OK response`() {
        val response = Response(
            summary = "OK",
            description = "Successful operation",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Response.serializer(), response)
    }

    @Test
    fun `should serialize 201 Created response with Location header`() {
        val response = Response(
            summary = "Created",
            description = "Resource successfully created",
            headers = mapOf(
                "Location" to Header(
                    description = "URL of the created resource",
                    schema = Schema(buildJsonObject {
                        put("type", "string")
                        put("format", "uri")
                    })
                )
            ),
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            )
        )
        val json = compactJson.encodeToString(response)

        assertTrue(json.contains("Location"))
        assertTrue(json.contains("created"))
    }

    @Test
    fun `should serialize 204 No Content response`() {
        val response = Response(
            summary = "No Content",
            description = "Successful operation with no response body"
        )
        val json = compactJson.encodeToString(response)

        assertEquals("""{"description":"Successful operation with no response body","summary":"No Content"}""", json)
    }

    @Test
    fun `should serialize 400 Bad Request response`() {
        val response = Response(
            summary = "Bad Request",
            description = "Invalid request parameters",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("error", buildJsonObject { put("type", "string") })
                            put("message", buildJsonObject { put("type", "string") })
                        })
                    })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Response.serializer(), response)
    }

    @Test
    fun `should serialize 404 Not Found response`() {
        val response = Response(
            summary = "Not Found",
            description = "The requested resource was not found",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("error", buildJsonObject { put("type", "string") })
                        })
                    })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Response.serializer(), response)
    }

    @Test
    fun `should serialize 500 Internal Server Error response`() {
        val response = Response(
            summary = "Internal Server Error",
            description = "An unexpected error occurred",
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("error", buildJsonObject { put("type", "string") })
                            put("timestamp", buildJsonObject { put("type", "string") })
                        })
                    })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Response.serializer(), response)
    }

    // Deserialization Tests

    @Test
    fun `should deserialize Response`() {
        val json = """{"description":"Success","summary":"OK","content":{"application/json":{"schema":{"type":"object"}}}}"""
        val response = compactJson.decodeFromString<Response>(json)

        assertEquals("Success", response.description)
        assertEquals("OK", response.summary)
        assertEquals(1, response.content?.size)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip Response with all properties`() {
        val response = Response(
            summary = "Successful response",
            description = "The operation completed successfully",
            headers = mapOf(
                "X-Total-Count" to Header(
                    description = "Total number of items",
                    schema = Schema(buildJsonObject { put("type", "integer") })
                )
            ),
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "object") })
                    })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Response.serializer(), response)
    }

    // Responses Tests

    @Test
    fun `should create Responses with specific status codes`() {
        val responses = responses(
            "200" to Response(description = "OK"),
            "404" to Response(description = "Not Found"),
            "500" to Response(description = "Internal Server Error")
        )

        assertEquals(3, responses.size)
        assertEquals("OK", responses["200"]?.description)
        assertEquals("Not Found", responses["404"]?.description)
        assertEquals("Internal Server Error", responses["500"]?.description)
    }

    @Test
    fun `should create Responses with wildcard patterns`() {
        val responses = responses(
            "2XX" to Response(description = "Success"),
            "4XX" to Response(description = "Client Error"),
            "5XX" to Response(description = "Server Error")
        )

        assertEquals(3, responses.size)
        assertEquals("Success", responses["2XX"]?.description)
        assertEquals("Client Error", responses["4XX"]?.description)
        assertEquals("Server Error", responses["5XX"]?.description)
    }

    @Test
    fun `should create Responses with default`() {
        val responses = responses(
            "200" to Response(description = "OK"),
            "default" to Response(description = "Unexpected error")
        )

        assertEquals(2, responses.size)
        assertEquals("OK", responses["200"]?.description)
        assertEquals("Unexpected error", responses["default"]?.description)
    }

    @Test
    fun `should create Responses with mixed status codes and patterns`() {
        val responses = responses(
            "200" to Response(description = "Success"),
            "201" to Response(description = "Created"),
            "4XX" to Response(description = "Client errors"),
            "5XX" to Response(description = "Server errors"),
            "default" to Response(description = "Default response")
        )

        assertEquals(5, responses.size)
    }

    @Test
    fun `should validate all wildcard patterns`() {
        val responses = responses(
            "1XX" to Response(description = "Informational"),
            "2XX" to Response(description = "Success"),
            "3XX" to Response(description = "Redirection"),
            "4XX" to Response(description = "Client Error"),
            "5XX" to Response(description = "Server Error")
        )

        assertEquals(5, responses.size)
    }

    @Test
    fun `should validate specific status codes in range 100-599`() {
        val responses = responses(
            "100" to Response(description = "Continue"),
            "200" to Response(description = "OK"),
            "301" to Response(description = "Moved Permanently"),
            "404" to Response(description = "Not Found"),
            "599" to Response(description = "Network Connect Timeout Error")
        )

        assertEquals(5, responses.size)
    }

    @Test
    fun `should reject empty Responses`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses()
        }

        assertEquals(
            "Responses must contain at least one response",
            exception.message
        )
    }

    @Test
    fun `should reject invalid status code - too low`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses("099" to Response(description = "Invalid"))
        }

        assertEquals(
            "Invalid status code: '099'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'",
            exception.message
        )
    }

    @Test
    fun `should reject invalid status code - too high`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses("600" to Response(description = "Invalid"))
        }

        assertEquals(
            "Invalid status code: '600'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'",
            exception.message
        )
    }

    @Test
    fun `should reject invalid wildcard pattern`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses("6XX" to Response(description = "Invalid"))
        }

        assertEquals(
            "Invalid status code: '6XX'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'",
            exception.message
        )
    }

    @Test
    fun `should reject invalid status code format`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses("abc" to Response(description = "Invalid"))
        }

        assertEquals(
            "Invalid status code: 'abc'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'",
            exception.message
        )
    }

    @Test
    fun `should reject status code with wrong number of digits`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses("20" to Response(description = "Invalid"))
        }

        assertEquals(
            "Invalid status code: '20'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'",
            exception.message
        )
    }

    // Serialize Responses Map

    @Test
    fun `should serialize Responses map`() {
        val responses: Responses = responses(
            "200" to Response(
                description = "Success",
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                )
            ),
            "404" to Response(
                description = "Not found"
            )
        )

        val json = compactJson.encodeToString(responses)

        assertTrue(json.contains("200"))
        assertTrue(json.contains("404"))
        assertTrue(json.contains("Success"))
        assertTrue(json.contains("Not found"))
    }

    @Test
    fun `should deserialize Responses map`() {
        val json = """{"200":{"description":"OK"},"404":{"description":"Not Found"}}"""
        val responses = compactJson.decodeFromString<Responses>(json)

        assertEquals(2, responses.size)
        assertEquals("OK", responses["200"]?.description)
        assertEquals("Not Found", responses["404"]?.description)
    }

    @Test
    fun `should round-trip Responses map`() {
        val responses: Responses = responses(
            "200" to Response(
                summary = "Success",
                description = "Successful operation",
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "array") })
                    )
                )
            ),
            "4XX" to Response(
                summary = "Client Error",
                description = "Client-side error"
            ),
            "default" to Response(
                summary = "Error",
                description = "Unexpected error"
            )
        )

        val json = compactJson.encodeToString(responses)
        val deserialized = compactJson.decodeFromString<Responses>(json)

        assertEquals(responses.size, deserialized.size)
        assertEquals(responses["200"]?.description, deserialized["200"]?.description)
        assertEquals(responses["4XX"]?.description, deserialized["4XX"]?.description)
        assertEquals(responses["default"]?.description, deserialized["default"]?.description)
    }

    // Complex Real-world Examples

    @Test
    fun `should serialize complete API endpoint responses`() {
        val responses: Responses = responses(
            "200" to Response(
                summary = "Success",
                description = "List of users retrieved successfully",
                headers = mapOf(
                    "X-Total-Count" to Header(
                        description = "Total number of users",
                        schema = Schema(buildJsonObject { put("type", "integer") })
                    ),
                    "X-Page" to Header(
                        description = "Current page number",
                        schema = Schema(buildJsonObject { put("type", "integer") })
                    )
                ),
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("id", buildJsonObject { put("type", "integer") })
                                    put("username", buildJsonObject { put("type", "string") })
                                })
                            })
                        })
                    )
                )
            ),
            "400" to Response(
                summary = "Bad Request",
                description = "Invalid query parameters",
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("error", buildJsonObject { put("type", "string") })
                            })
                        })
                    )
                )
            ),
            "401" to Response(
                summary = "Unauthorized",
                description = "Authentication required"
            ),
            "500" to Response(
                summary = "Server Error",
                description = "Internal server error",
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("error", buildJsonObject { put("type", "string") })
                                put("timestamp", buildJsonObject { put("type", "string") })
                            })
                        })
                    )
                )
            )
        )

        val json = compactJson.encodeToString(responses)
        assertTrue(json.contains("200"))
        assertTrue(json.contains("400"))
        assertTrue(json.contains("401"))
        assertTrue(json.contains("500"))
    }
}
