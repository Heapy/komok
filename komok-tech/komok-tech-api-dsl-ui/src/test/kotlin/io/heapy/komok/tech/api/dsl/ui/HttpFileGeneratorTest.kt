package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.*
import io.heapy.komok.tech.logging.Logger
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HttpFileGeneratorTest {

    private companion object : Logger()

    @Test
    fun `should generate HTTP file with header and base URL from server`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            servers = listOf(
                Server(url = "https://api.example.com/v1")
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List users",
                        responses = mapOf("200" to Response(description = "Success"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(httpFile.contains("# Test API - HTTP Requests"), "Should contain title comment")
        assertTrue(httpFile.contains("# Version: 1.0.0"), "Should contain version comment")
        assertTrue(httpFile.contains("@baseUrl = https://api.example.com/v1"), "Should use first server as base URL")
    }

    @Test
    fun `should use placeholder when no servers defined`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/test" to PathItem(
                    get = Operation(
                        summary = "Test",
                        responses = mapOf("200" to Response(description = "OK"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(httpFile.contains("@baseUrl = {{baseUrl}}"), "Should use placeholder when no servers")
    }

    @Test
    fun `should generate request for each operation with documentation`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List users",
                        description = "Returns a list of all users",
                        responses = mapOf(
                            "200" to Response(description = "Success"),
                            "401" to Response(description = "Unauthorized")
                        )
                    ),
                    post = Operation(
                        summary = "Create user",
                        description = "Creates a new user account",
                        requestBody = RequestBody(
                            content = mapOf("application/json" to MediaType())
                        ),
                        responses = mapOf("201" to Response(description = "Created"))
                    )
                ),
                "/users/{id}" to PathItem(
                    get = Operation(
                        summary = "Get user by ID",
                        parameters = listOf(
                            Direct(
                                Parameter(
                                    name = "id",
                                    location = ParameterLocation.PATH,
                                    description = "User identifier",
                                    required = true,
                                    schema = Schema(buildJsonObject { put("type", "integer") })
                                )
                            )
                        ),
                        responses = mapOf("200" to Response(description = "Success"))
                    ),
                    delete = Operation(
                        summary = "Delete user",
                        responses = mapOf("204" to Response(description = "No Content"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        // Verify operation sections
        assertTrue(httpFile.contains("### List users"), "Should contain GET /users summary")
        assertTrue(httpFile.contains("# Returns a list of all users"), "Should contain description as comment")
        assertTrue(httpFile.contains("GET {{baseUrl}}/users"), "Should contain GET /users request")

        assertTrue(httpFile.contains("### Create user"), "Should contain POST /users summary")
        assertTrue(httpFile.contains("# Creates a new user account"), "Should contain POST description")
        assertTrue(httpFile.contains("POST {{baseUrl}}/users"), "Should contain POST /users request")
        assertTrue(httpFile.contains("Content-Type: application/json"), "Should contain Content-Type header for POST")

        assertTrue(httpFile.contains("### Get user by ID"), "Should contain GET /users/{id} summary")
        assertTrue(httpFile.contains("# @param {id} - User identifier"), "Should document path parameter")
        assertTrue(httpFile.contains("GET {{baseUrl}}/users/{{id}}"), "Should replace path parameters with templates")

        // Verify response documentation
        assertTrue(httpFile.contains("# 200: Success"), "Should document 200 response")
        assertTrue(httpFile.contains("# 401: Unauthorized"), "Should document 401 response")
        assertTrue(httpFile.contains("# 201: Created"), "Should document 201 response")
    }

    @Test
    fun `should include query parameters in URL`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/pets" to PathItem(
                    get = Operation(
                        summary = "Find pets by status",
                        parameters = listOf(
                            Direct(
                                Parameter(
                                    name = "status",
                                    location = ParameterLocation.QUERY,
                                    description = "Status filter",
                                    required = true,
                                    schema = Schema(buildJsonObject { put("type", "string") })
                                )
                            ),
                            Direct(
                                Parameter(
                                    name = "limit",
                                    location = ParameterLocation.QUERY,
                                    description = "Max results",
                                    required = false,
                                    schema = Schema(buildJsonObject { put("type", "integer") })
                                )
                            )
                        ),
                        responses = mapOf("200" to Response(description = "Success"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(
            httpFile.contains("GET {{baseUrl}}/pets?status={{status}}&limit={{limit}}"),
            "Should append query parameters to URL"
        )
    }

    @Test
    fun `should include example request body when available`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    post = Operation(
                        summary = "Create user",
                        requestBody = RequestBody(
                            content = mapOf(
                                "application/json" to MediaType(
                                    example = buildJsonObject {
                                        put("name", "John Doe")
                                        put("email", "john@example.com")
                                    }
                                )
                            )
                        ),
                        responses = mapOf("201" to Response(description = "Created"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(httpFile.contains("\"name\": \"John Doe\""), "Should contain example body with name")
        assertTrue(httpFile.contains("\"email\": \"john@example.com\""), "Should contain example body with email")
    }

    @Test
    fun `should generate example body from component schema`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            components = Components(
                schemas = mapOf(
                    "User" to Schema(
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("id") {
                                    put("type", "integer")
                                    put("example", 10)
                                }
                                putJsonObject("name") {
                                    put("type", "string")
                                    put("example", "Alice")
                                }
                                putJsonObject("email") {
                                    put("type", "string")
                                    put("format", "email")
                                }
                            }
                        }
                    )
                )
            ),
            paths = mapOf(
                "/users" to PathItem(
                    post = Operation(
                        summary = "Create user",
                        requestBody = RequestBody(
                            content = mapOf(
                                "application/json" to MediaType(
                                    schema = Schema(
                                        buildJsonObject {
                                            put("\$ref", "#/components/schemas/User")
                                        }
                                    )
                                )
                            )
                        ),
                        responses = mapOf("201" to Response(description = "Created"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        // Should generate body from schema examples and type defaults
        assertTrue(httpFile.contains("\"id\": 10"), "Should use example value for id")
        assertTrue(httpFile.contains("\"name\": \"Alice\""), "Should use example value for name")
        assertTrue(httpFile.contains("\"email\": \"user@example.com\""), "Should use format default for email")
    }

    @Test
    fun `should include Accept header when responses have content`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List users",
                        responses = mapOf(
                            "200" to Response(
                                description = "Success",
                                content = mapOf(
                                    "application/json" to MediaType()
                                )
                            )
                        )
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(httpFile.contains("Accept: application/json"), "Should include Accept header")
    }

    @Test
    fun `should use operation summary as comment and fallback to method + path`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List all users",
                        responses = mapOf("200" to Response(description = "OK"))
                    )
                ),
                "/health" to PathItem(
                    get = Operation(
                        responses = mapOf("200" to Response(description = "OK"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(httpFile.contains("### List all users"), "Should use summary as comment")
        assertTrue(httpFile.contains("### GET /health"), "Should fallback to method + path when no summary")
    }

    @Test
    fun `should list multiple servers with first active and rest commented`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Multi-Server API",
                version = "1.0.0"
            ),
            servers = listOf(
                Server(url = "https://api.example.com/v1", description = "Production"),
                Server(url = "https://staging.example.com/v1", description = "Staging"),
                Server(url = "http://localhost:8080", description = "Local")
            ),
            paths = mapOf(
                "/test" to PathItem(
                    get = Operation(
                        summary = "Test",
                        responses = mapOf("200" to Response(description = "OK"))
                    )
                )
            )
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(
            httpFile.contains("@baseUrl = https://api.example.com/v1 # Production"),
            "First server should be active"
        )
        assertTrue(
            httpFile.contains("# @baseUrl = https://staging.example.com/v1 # Staging"),
            "Second server should be commented out"
        )
        assertTrue(
            httpFile.contains("# @baseUrl = http://localhost:8080 # Local"),
            "Third server should be commented out"
        )
    }

    @Test
    fun `should generate empty content for API without paths`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Empty API",
                version = "1.0.0"
            ),
            paths = emptyMap()
        )

        val httpFile = generateHttpFile(openapi)

        assertTrue(httpFile.contains("# Empty API - HTTP Requests"), "Should still contain title")
        assertTrue(httpFile.contains("@baseUrl"), "Should still contain baseUrl variable")
        assertFalse(httpFile.contains("GET "), "Should not contain any GET requests")
        assertFalse(httpFile.contains("POST "), "Should not contain any POST requests")
    }
}
