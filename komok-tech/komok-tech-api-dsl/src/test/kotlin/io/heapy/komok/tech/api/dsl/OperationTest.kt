package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OperationTest {

    // Basic Operation Tests

    @Test
    fun `should serialize minimal Operation with responses`() {
        val operation = Operation(
            responses = responses(
                "200" to Response(description = "Success")
            )
        )
        val json = compactJson.encodeToString(operation)

        val expected = """{"responses":{"200":{"description":"Success"}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Operation with summary and description`() {
        val operation = Operation(
            summary = "List users",
            description = "Returns a list of all users in the system",
            responses = responses(
                "200" to Response(description = "Success")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("List users"))
        assertTrue(json.contains("Returns a list of all users"))
    }

    @Test
    fun `should serialize Operation with tags`() {
        val operation = Operation(
            tags = listOf("users", "admin"),
            summary = "Get user",
            responses = responses(
                "200" to Response(description = "Success")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("users"))
        assertTrue(json.contains("admin"))
    }

    @Test
    fun `should serialize Operation with operationId`() {
        val operation = Operation(
            operationId = "listUsers",
            summary = "List all users",
            responses = responses(
                "200" to Response(description = "List of users")
            )
        )
        val json = compactJson.encodeToString(operation)

        val expected = """{"responses":{"200":{"description":"List of users"}},"summary":"List all users","operationId":"listUsers"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Operation with externalDocs`() {
        val operation = Operation(
            summary = "Create user",
            externalDocs = ExternalDocumentation(
                description = "Find more info here",
                url = "https://example.com/docs/create-user"
            ),
            responses = responses(
                "201" to Response(description = "Created")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("externalDocs"))
        assertTrue(json.contains("Find more info here"))
    }

    // Parameters Tests

    @Test
    fun `should serialize Operation with parameters`() {
        val operation = Operation(
            summary = "Get user by ID",
            parameters = listOf(
                Parameter(
                    name = "userId",
                    location = ParameterLocation.PATH,
                    description = "User identifier",
                    required = true,
                    schema = Schema(buildJsonObject {
                        put("type", "string")
                        put("format", "uuid")
                    })
                ),
                Parameter(
                    name = "includeDeleted",
                    location = ParameterLocation.QUERY,
                    description = "Include deleted users",
                    schema = Schema(buildJsonObject {
                        put("type", "boolean")
                        put("default", false)
                    })
                )
            ),
            responses = responses(
                "200" to Response(description = "User found"),
                "404" to Response(description = "User not found")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("userId"))
        assertTrue(json.contains("includeDeleted"))
        assertTrue(json.contains("parameters"))
    }

    // Request Body Tests

    @Test
    fun `should serialize Operation with requestBody`() {
        val operation = Operation(
            summary = "Create user",
            requestBody = RequestBody(
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("username", buildJsonObject { put("type", "string") })
                                put("email", buildJsonObject { put("type", "string") })
                            })
                        })
                    )
                ),
                description = "User to create",
                required = true
            ),
            responses = responses(
                "201" to Response(description = "User created")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("requestBody"))
        assertTrue(json.contains("User to create"))
    }

    // Responses Tests

    @Test
    fun `should serialize Operation with multiple responses`() {
        val operation = Operation(
            summary = "Update user",
            responses = responses(
                "200" to Response(
                    description = "User updated successfully",
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject { put("type", "object") })
                        )
                    )
                ),
                "400" to Response(description = "Invalid input"),
                "404" to Response(description = "User not found"),
                "500" to Response(description = "Internal server error")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("200"))
        assertTrue(json.contains("400"))
        assertTrue(json.contains("404"))
        assertTrue(json.contains("500"))
    }

    @Test
    fun `should serialize Operation with wildcard responses`() {
        val operation = Operation(
            summary = "Delete user",
            responses = responses(
                "204" to Response(description = "Deleted successfully"),
                "4XX" to Response(description = "Client error"),
                "5XX" to Response(description = "Server error")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("204"))
        assertTrue(json.contains("4XX"))
        assertTrue(json.contains("5XX"))
    }

    // Deprecated Flag Tests

    @Test
    fun `should serialize Operation with deprecated flag`() {
        val operation = Operation(
            summary = "Old endpoint",
            deprecated = true,
            responses = responses(
                "200" to Response(description = "Success")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("\"deprecated\":true"))
    }

    @Test
    fun `should not serialize deprecated when false`() {
        val operation = Operation(
            summary = "Current endpoint",
            deprecated = false,
            responses = responses(
                "200" to Response(description = "Success")
            )
        )
        val json = compactJson.encodeToString(operation)

        // deprecated should not appear when false (default value)
        assertTrue(!json.contains("deprecated"))
    }

    // Servers Tests

    @Test
    fun `should serialize Operation with custom servers`() {
        val operation = Operation(
            summary = "Special endpoint",
            servers = listOf(
                Server(
                    url = "https://api-v2.example.com",
                    description = "V2 API server"
                ),
                Server(
                    url = "https://staging-api.example.com",
                    description = "Staging server"
                )
            ),
            responses = responses(
                "200" to Response(description = "Success")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("servers"))
        assertTrue(json.contains("api-v2.example.com"))
        assertTrue(json.contains("staging-api.example.com"))
    }

    // Callbacks Tests

    @Test
    fun `should serialize Operation with callbacks`() {
        val operation = Operation(
            summary = "Subscribe to webhook",
            callbacks = mapOf(
                "orderUpdate" to mapOf(
                    "{${'$'}request.body#/callbackUrl}" to PathItem(
                        post = Operation(
                            summary = "Order status changed",
                            requestBody = RequestBody(
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject {
                                            put("type", "object")
                                            put("properties", buildJsonObject {
                                                put("orderId", buildJsonObject { put("type", "string") })
                                                put("status", buildJsonObject { put("type", "string") })
                                            })
                                        })
                                    )
                                )
                            ),
                            responses = responses(
                                "200" to Response(description = "Callback received")
                            )
                        )
                    )
                )
            ),
            responses = responses(
                "201" to Response(description = "Subscription created")
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("callbacks"))
        assertTrue(json.contains("orderUpdate"))
    }

    // Specification Extensions Tests

    @Test
    fun `should serialize Operation with specification extensions`() {
        val operation = Operation(
            summary = "Custom operation",
            responses = responses(
                "200" to Response(description = "Success")
            ),
            extensions = mapOf(
                "x-internal-id" to JsonPrimitive("op-123"),
                "x-rate-limit" to JsonPrimitive(100)
            )
        )
        val json = compactJson.encodeToString(operation)

        assertTrue(json.contains("x-internal-id"))
        assertTrue(json.contains("op-123"))
        assertTrue(json.contains("x-rate-limit"))
    }

    // Validation Tests

    @Test
    fun `should reject Operation with empty responses`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Operation(
                summary = "Invalid operation",
                responses = emptyMap()
            )
        }

        assertEquals(
            "Operation must have at least one response defined",
            exception.message
        )
    }

    // Deserialization Tests

    @Test
    fun `should deserialize Operation`() {
        val json = """{"summary":"List users","operationId":"listUsers","responses":{"200":{"description":"Success"}}}"""
        val operation = compactJson.decodeFromString<Operation>(json)

        assertEquals(
            Operation(
                summary = "List users",
                operationId = "listUsers",
                responses = responses("200" to Response(description = "Success"))
            ),
            operation
        )
    }

    @Test
    fun `should deserialize Operation with default deprecated value`() {
        val json = """{"responses":{"200":{"description":"OK"}}}"""
        val operation = compactJson.decodeFromString<Operation>(json)

        assertEquals(false, operation.deprecated)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip Operation with all properties`() {
        val operation = Operation(
            tags = listOf("users", "admin"),
            summary = "Create user",
            description = "Creates a new user in the system",
            externalDocs = ExternalDocumentation(
                url = "https://example.com/docs"
            ),
            operationId = "createUser",
            parameters = listOf(
                Parameter(
                    name = "X-Request-ID",
                    location = ParameterLocation.HEADER,
                    schema = Schema(buildJsonObject { put("type", "string") })
                )
            ),
            requestBody = RequestBody(
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                ),
                required = true
            ),
            responses = responses(
                "201" to Response(
                    description = "User created",
                    headers = mapOf(
                        "Location" to Direct(Header(
                            description = "URL of created user",
                            schema = Schema(buildJsonObject { put("type", "string") })
                        ))
                    )
                ),
                "400" to Response(description = "Invalid input")
            ),
            deprecated = false,
            servers = listOf(
                Server(url = "https://api.example.com")
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Operation.serializer(), operation)
    }

    // Complex Real-world Examples

    @Test
    fun `should serialize complete REST API operation`() {
        val operation = Operation(
            tags = listOf("pets"),
            summary = "Find pets by status",
            description = "Multiple status values can be provided with comma separated strings",
            operationId = "findPetsByStatus",
            parameters = listOf(
                Parameter(
                    name = "status",
                    location = ParameterLocation.QUERY,
                    description = "Status values that need to be considered for filter",
                    required = true,
                    schema = Schema(buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject {
                            put("type", "string")
                            put("enum", buildJsonObject {
                                put("0", "available")
                                put("1", "pending")
                                put("2", "sold")
                            })
                        })
                    }),
                    style = ParameterStyle.FORM,
                    explode = true
                )
            ),
            responses = responses(
                "200" to Response(
                    description = "Successful operation",
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject {
                                put("type", "array")
                                put("items", buildJsonObject {
                                    put("\$ref", "#/components/schemas/Pet")
                                })
                            })
                        ),
                        "application/xml" to MediaType(
                            schema = Schema(buildJsonObject {
                                put("type", "array")
                                put("items", buildJsonObject {
                                    put("\$ref", "#/components/schemas/Pet")
                                })
                            })
                        )
                    )
                ),
                "400" to Response(description = "Invalid status value")
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Operation.serializer(), operation)
    }

    @Test
    fun `should serialize operation with nested callbacks`() {
        val operation = Operation(
            summary = "Register webhook",
            description = "Registers a webhook to receive notifications",
            operationId = "registerWebhook",
            requestBody = RequestBody(
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("url", buildJsonObject {
                                    put("type", "string")
                                    put("format", "uri")
                                })
                                put("events", buildJsonObject {
                                    put("type", "array")
                                    put("items", buildJsonObject { put("type", "string") })
                                })
                            })
                        })
                    )
                ),
                required = true
            ),
            responses = responses(
                "201" to Response(
                    description = "Webhook registered",
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("webhookId", buildJsonObject { put("type", "string") })
                                })
                            })
                        )
                    )
                )
            ),
            callbacks = mapOf(
                "notification" to mapOf(
                    "{${'$'}request.body#/url}" to PathItem(
                        post = Operation(
                            summary = "Notification callback",
                            requestBody = RequestBody(
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject {
                                            put("type", "object")
                                            put("properties", buildJsonObject {
                                                put("event", buildJsonObject { put("type", "string") })
                                                put("data", buildJsonObject { put("type", "object") })
                                            })
                                        })
                                    )
                                )
                            ),
                            responses = responses(
                                "200" to Response(description = "Notification acknowledged")
                            )
                        )
                    )
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Operation.serializer(), operation)
    }
}
