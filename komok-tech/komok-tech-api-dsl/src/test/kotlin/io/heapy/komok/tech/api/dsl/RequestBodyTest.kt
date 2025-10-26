package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RequestBodyTest {

    // Basic RequestBody Tests

    @Test
    fun `should serialize minimal RequestBody`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            )
        )
        val json = compactJson.encodeToString(requestBody)

        val expected = """{"content":{"application/json":{"schema":{"type":"object"}}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize RequestBody with description`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            ),
            description = "User object to be created"
        )
        val json = compactJson.encodeToString(requestBody)

        val expected = """{"content":{"application/json":{"schema":{"type":"object"}}},"description":"User object to be created"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize RequestBody with required flag`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            ),
            description = "Pet to add to the store",
            required = true
        )
        val json = compactJson.encodeToString(requestBody)

        val expected = """{"content":{"application/json":{"schema":{"type":"object"}}},"description":"Pet to add to the store","required":true}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize RequestBody with multiple content types`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                ),
                "application/xml" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                ),
                "text/plain" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "string") })
                )
            ),
            description = "User data in various formats"
        )
        val json = compactJson.encodeToString(requestBody)

        // Verify it contains all content types
        assert(json.contains("application/json"))
        assert(json.contains("application/xml"))
        assert(json.contains("text/plain"))
    }

    @Test
    fun `should serialize RequestBody with examples`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("username", buildJsonObject { put("type", "string") })
                            put("email", buildJsonObject { put("type", "string") })
                        })
                    }),
                    examples = mapOf(
                        "user1" to Example(
                            summary = "User example 1",
                            value = buildJsonObject {
                                put("username", "johndoe")
                                put("email", "john@example.com")
                            }
                        ),
                        "user2" to Example(
                            summary = "User example 2",
                            value = buildJsonObject {
                                put("username", "janedoe")
                                put("email", "jane@example.com")
                            }
                        )
                    )
                )
            ),
            description = "Create a new user"
        )
        val json = compactJson.encodeToString(requestBody)

        assert(json.contains("user1"))
        assert(json.contains("user2"))
        assert(json.contains("johndoe"))
        assert(json.contains("janedoe"))
    }

    @Test
    fun `should serialize RequestBody with specification extensions`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            ),
            description = "Custom request body",
            extensions = mapOf("x-internal-id" to JsonPrimitive("rb-123"))
        )
        val json = compactJson.encodeToString(requestBody)

        val expected = """{"content":{"application/json":{"schema":{"type":"object"}}},"description":"Custom request body","extensions":{"x-internal-id":"rb-123"}}"""
        assertEquals(expected, json)
    }

    // Content Type Tests

    @Test
    fun `should serialize RequestBody with form data`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/x-www-form-urlencoded" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("username", buildJsonObject { put("type", "string") })
                            put("password", buildJsonObject { put("type", "string") })
                        })
                    })
                )
            ),
            description = "Login credentials"
        )
        val json = compactJson.encodeToString(requestBody)

        assert(json.contains("application/x-www-form-urlencoded"))
        assert(json.contains("username"))
        assert(json.contains("password"))
    }

    @Test
    fun `should serialize RequestBody with multipart form data`() {
        val requestBody = RequestBody(
            content = mapOf(
                "multipart/form-data" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("file", buildJsonObject {
                                put("type", "string")
                                put("format", "binary")
                            })
                            put("description", buildJsonObject { put("type", "string") })
                        })
                    }),
                    encoding = mapOf(
                        "file" to Encoding(
                            contentType = "image/png, image/jpeg"
                        )
                    )
                )
            ),
            description = "Upload file with metadata"
        )
        val json = compactJson.encodeToString(requestBody)

        assert(json.contains("multipart/form-data"))
        assert(json.contains("binary"))
        assert(json.contains("image/png"))
    }

    @Test
    fun `should serialize RequestBody with binary data`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/octet-stream" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "string")
                        put("format", "binary")
                    })
                )
            ),
            description = "Raw binary data"
        )
        val json = compactJson.encodeToString(requestBody)

        assert(json.contains("application/octet-stream"))
        assert(json.contains("binary"))
    }

    // Validation Tests

    @Test
    fun `should reject RequestBody with empty content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RequestBody(
                content = emptyMap(),
                description = "Invalid request body"
            )
        }

        assertEquals(
            "RequestBody 'content' must not be empty",
            exception.message
        )
    }

    // Deserialization Tests

    @Test
    fun `should deserialize RequestBody`() {
        val json = """{"content":{"application/json":{"schema":{"type":"object"}}},"description":"User data","required":true}"""
        val requestBody = compactJson.decodeFromString<RequestBody>(json)

        assertEquals("User data", requestBody.description)
        assertEquals(true, requestBody.required)
        assertEquals(1, requestBody.content.size)
        assert(requestBody.content.containsKey("application/json"))
    }

    @Test
    fun `should deserialize RequestBody with default required value`() {
        val json = """{"content":{"application/json":{"schema":{"type":"string"}}}}"""
        val requestBody = compactJson.decodeFromString<RequestBody>(json)

        assertEquals(false, requestBody.required)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip RequestBody with single content type`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("required", buildJsonObject {
                            put("name", "string")
                        })
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                            put("age", buildJsonObject { put("type", "integer") })
                        })
                    })
                )
            ),
            description = "User registration data",
            required = true
        )

        TestHelpers.testRoundTripWithoutValidation(RequestBody.serializer(), requestBody)
    }

    @Test
    fun `should round-trip RequestBody with multiple content types`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                ),
                "application/xml" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            ),
            description = "Data in multiple formats"
        )

        TestHelpers.testRoundTripWithoutValidation(RequestBody.serializer(), requestBody)
    }

    // Complex Real-world Examples

    @Test
    fun `should serialize complex RequestBody for user creation`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("required", buildJsonObject {
                            put("0", "username")
                            put("1", "email")
                        })
                        put("properties", buildJsonObject {
                            put("username", buildJsonObject {
                                put("type", "string")
                                put("minLength", 3)
                                put("maxLength", 50)
                            })
                            put("email", buildJsonObject {
                                put("type", "string")
                                put("format", "email")
                            })
                            put("fullName", buildJsonObject {
                                put("type", "string")
                            })
                            put("age", buildJsonObject {
                                put("type", "integer")
                                put("minimum", 18)
                            })
                        })
                    }),
                    example = buildJsonObject {
                        put("username", "johndoe")
                        put("email", "john@example.com")
                        put("fullName", "John Doe")
                        put("age", 30)
                    }
                )
            ),
            description = "User registration data with validation",
            required = true
        )

        TestHelpers.testRoundTripWithoutValidation(RequestBody.serializer(), requestBody)
    }

    @Test
    fun `should serialize RequestBody with nested schema`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("user", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("name", buildJsonObject { put("type", "string") })
                                    put("email", buildJsonObject { put("type", "string") })
                                })
                            })
                            put("metadata", buildJsonObject {
                                put("type", "object")
                                put("additionalProperties", true)
                            })
                        })
                    })
                )
            ),
            description = "Nested user data with metadata"
        )

        TestHelpers.testRoundTripWithoutValidation(RequestBody.serializer(), requestBody)
    }

    @Test
    fun `should serialize RequestBody with array schema`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("id", buildJsonObject { put("type", "integer") })
                                put("name", buildJsonObject { put("type", "string") })
                            })
                        })
                    }),
                    example = buildJsonObject {
                        put("0", buildJsonObject {
                            put("id", 1)
                            put("name", "Item 1")
                        })
                        put("1", buildJsonObject {
                            put("id", 2)
                            put("name", "Item 2")
                        })
                    }
                )
            ),
            description = "Array of items to create",
            required = true
        )

        TestHelpers.testRoundTripWithoutValidation(RequestBody.serializer(), requestBody)
    }
}
