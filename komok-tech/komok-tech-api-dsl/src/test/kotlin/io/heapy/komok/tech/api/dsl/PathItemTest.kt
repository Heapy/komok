package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PathItemTest {

    // Basic PathItem Tests

    @Test
    fun `should serialize minimal PathItem with GET operation`() {
        val pathItem = PathItem(
            get = Operation(
                summary = "List items",
                responses = responses("200" to Response(description = "Success"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"get\""))
        assertTrue(json.contains("List items"))
    }

    @Test
    fun `should serialize PathItem with summary and description`() {
        val pathItem = PathItem(
            summary = "User operations",
            description = "Operations for managing users",
            get = Operation(
                responses = responses("200" to Response(description = "OK"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("User operations"))
        assertTrue(json.contains("Operations for managing users"))
    }

    @Test
    fun `should serialize PathItem with ref`() {
        val pathItem = PathItem(
            ref = "#/components/pathItems/UserPath"
        )
        val json = compactJson.encodeToString(pathItem)

        val expected = """{"${'$'}ref":"#/components/pathItems/UserPath"}"""
        assertEquals(expected, json)
    }

    // HTTP Methods Tests

    @Test
    fun `should serialize PathItem with GET operation`() {
        val pathItem = PathItem(
            get = Operation(
                summary = "Get resource",
                operationId = "getResource",
                responses = responses("200" to Response(description = "Resource retrieved"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"get\""))
        assertTrue(json.contains("getResource"))
    }

    @Test
    fun `should serialize PathItem with PUT operation`() {
        val pathItem = PathItem(
            put = Operation(
                summary = "Update resource",
                operationId = "updateResource",
                responses = responses("200" to Response(description = "Resource updated"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"put\""))
        assertTrue(json.contains("updateResource"))
    }

    @Test
    fun `should serialize PathItem with POST operation`() {
        val pathItem = PathItem(
            post = Operation(
                summary = "Create resource",
                operationId = "createResource",
                responses = responses("201" to Response(description = "Resource created"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"post\""))
        assertTrue(json.contains("createResource"))
    }

    @Test
    fun `should serialize PathItem with DELETE operation`() {
        val pathItem = PathItem(
            delete = Operation(
                summary = "Delete resource",
                operationId = "deleteResource",
                responses = responses("204" to Response(description = "Resource deleted"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"delete\""))
        assertTrue(json.contains("deleteResource"))
    }

    @Test
    fun `should serialize PathItem with OPTIONS operation`() {
        val pathItem = PathItem(
            options = Operation(
                summary = "Get options",
                operationId = "getOptions",
                responses = responses("200" to Response(description = "Options retrieved"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"options\""))
        assertTrue(json.contains("getOptions"))
    }

    @Test
    fun `should serialize PathItem with HEAD operation`() {
        val pathItem = PathItem(
            head = Operation(
                summary = "Get headers",
                operationId = "getHeaders",
                responses = responses("200" to Response(description = "Headers retrieved"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"head\""))
        assertTrue(json.contains("getHeaders"))
    }

    @Test
    fun `should serialize PathItem with PATCH operation`() {
        val pathItem = PathItem(
            patch = Operation(
                summary = "Partially update resource",
                operationId = "patchResource",
                responses = responses("200" to Response(description = "Resource patched"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"patch\""))
        assertTrue(json.contains("patchResource"))
    }

    @Test
    fun `should serialize PathItem with TRACE operation`() {
        val pathItem = PathItem(
            trace = Operation(
                summary = "Trace request",
                operationId = "traceRequest",
                responses = responses("200" to Response(description = "Trace information"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"trace\""))
        assertTrue(json.contains("traceRequest"))
    }

    @Test
    fun `should serialize PathItem with QUERY operation`() {
        val pathItem = PathItem(
            query = Operation(
                summary = "Query resource",
                operationId = "queryResource",
                responses = responses("200" to Response(description = "Query results"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"query\""))
        assertTrue(json.contains("queryResource"))
    }

    @Test
    fun `should serialize PathItem with all HTTP methods`() {
        val pathItem = PathItem(
            get = Operation(responses = responses("200" to Response(description = "GET"))),
            put = Operation(responses = responses("200" to Response(description = "PUT"))),
            post = Operation(responses = responses("201" to Response(description = "POST"))),
            delete = Operation(responses = responses("204" to Response(description = "DELETE"))),
            options = Operation(responses = responses("200" to Response(description = "OPTIONS"))),
            head = Operation(responses = responses("200" to Response(description = "HEAD"))),
            patch = Operation(responses = responses("200" to Response(description = "PATCH"))),
            trace = Operation(responses = responses("200" to Response(description = "TRACE"))),
            query = Operation(responses = responses("200" to Response(description = "QUERY")))
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("\"get\""))
        assertTrue(json.contains("\"put\""))
        assertTrue(json.contains("\"post\""))
        assertTrue(json.contains("\"delete\""))
        assertTrue(json.contains("\"options\""))
        assertTrue(json.contains("\"head\""))
        assertTrue(json.contains("\"patch\""))
        assertTrue(json.contains("\"trace\""))
        assertTrue(json.contains("\"query\""))
    }

    // Parameters Tests

    @Test
    fun `should serialize PathItem with parameters`() {
        val pathItem = PathItem(
            parameters = listOf(
                Direct(Parameter(
                    name = "id",
                    location = ParameterLocation.PATH,
                    description = "Resource ID",
                    required = true,
                    schema = Schema(buildJsonObject { put("type", "string") })
                )),
                Direct(Parameter(
                    name = "version",
                    location = ParameterLocation.HEADER,
                    description = "API version",
                    schema = Schema(buildJsonObject { put("type", "string") })
                ))
            ),
            get = Operation(
                responses = responses("200" to Response(description = "Success"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("parameters"))
        assertTrue(json.contains("id"))
        assertTrue(json.contains("version"))
    }

    // Servers Tests

    @Test
    fun `should serialize PathItem with custom servers`() {
        val pathItem = PathItem(
            servers = listOf(
                Server(
                    url = "https://api-v2.example.com",
                    description = "V2 API"
                ),
                Server(
                    url = "https://staging.example.com",
                    description = "Staging"
                )
            ),
            get = Operation(
                responses = responses("200" to Response(description = "Success"))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("servers"))
        assertTrue(json.contains("api-v2.example.com"))
        assertTrue(json.contains("staging.example.com"))
    }

    // Additional Operations Tests

    @Test
    fun `should serialize PathItem with additionalOperations`() {
        val pathItem = PathItem(
            additionalOperations = mapOf(
                "CUSTOM" to Operation(
                    summary = "Custom operation",
                    responses = responses("200" to Response(description = "Custom response"))
                ),
                "SPECIAL" to Operation(
                    summary = "Special operation",
                    responses = responses("200" to Response(description = "Special response"))
                )
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("additionalOperations"))
        assertTrue(json.contains("CUSTOM"))
        assertTrue(json.contains("SPECIAL"))
    }

    @Test
    fun `should serialize PathItem with lowercase additional operation`() {
        val pathItem = PathItem(
            additionalOperations = mapOf(
                "custom" to Operation(
                    responses = responses("200" to Response(description = "OK"))
                )
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("custom"))
    }

    @Test
    fun `should serialize PathItem with RFC9110 compliant method names`() {
        val pathItem = PathItem(
            additionalOperations = mapOf(
                "SEARCH" to Operation(responses = responses("200" to Response(description = "OK"))),
                "LOCK" to Operation(responses = responses("200" to Response(description = "OK"))),
                "UNLOCK" to Operation(responses = responses("200" to Response(description = "OK"))),
                "custom-method" to Operation(responses = responses("200" to Response(description = "OK"))),
                "method.name" to Operation(responses = responses("200" to Response(description = "OK")))
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("SEARCH"))
        assertTrue(json.contains("LOCK"))
        assertTrue(json.contains("custom-method"))
    }

    // Validation Tests

    @Test
    fun `should reject PathItem with standard method in additionalOperations`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PathItem(
                additionalOperations = mapOf(
                    "GET" to Operation(responses = responses("200" to Response(description = "OK")))
                )
            )
        }

        assertTrue(exception.message?.contains("GET") == true)
        assertTrue(exception.message?.contains("cannot be a standard HTTP method") == true)
    }

    @Test
    fun `should reject PathItem with POST in additionalOperations`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PathItem(
                additionalOperations = mapOf(
                    "POST" to Operation(responses = responses("200" to Response(description = "OK")))
                )
            )
        }

        assertTrue(exception.message?.contains("POST") == true)
    }

    @Test
    fun `should reject PathItem with invalid method name pattern`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PathItem(
                additionalOperations = mapOf(
                    "INVALID METHOD" to Operation(responses = responses("200" to Response(description = "OK")))
                )
            )
        }

        assertTrue(exception.message?.contains("INVALID METHOD") == true)
        assertTrue(exception.message?.contains("RFC9110") == true)
    }

    @Test
    fun `should reject PathItem with method containing invalid characters`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PathItem(
                additionalOperations = mapOf(
                    "METHOD@INVALID" to Operation(responses = responses("200" to Response(description = "OK")))
                )
            )
        }

        assertTrue(exception.message?.contains("METHOD@INVALID") == true)
    }

    // Specification Extensions Tests

    @Test
    fun `should serialize PathItem with specification extensions`() {
        val pathItem = PathItem(
            get = Operation(
                responses = responses("200" to Response(description = "OK"))
            ),
            extensions = mapOf(
                "x-internal-id" to JsonPrimitive("path-123"),
                "x-rate-limit" to JsonPrimitive(100)
            )
        )
        val json = compactJson.encodeToString(pathItem)

        assertTrue(json.contains("x-internal-id"))
        assertTrue(json.contains("path-123"))
    }

    // Deserialization Tests

    @Test
    fun `should deserialize PathItem`() {
        val json = """{"summary":"User operations","get":{"responses":{"200":{"description":"OK"}}}}"""
        val pathItem = compactJson.decodeFromString<PathItem>(json)

        assertEquals("User operations", pathItem.summary)
        assertEquals("OK", pathItem.get?.responses?.get("200")?.description)
    }

    @Test
    fun `should deserialize PathItem with ref`() {
        val json = """{"${'$'}ref":"#/components/pathItems/Common"}"""
        val pathItem = compactJson.decodeFromString<PathItem>(json)

        assertEquals("#/components/pathItems/Common", pathItem.ref)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip PathItem with single operation`() {
        val pathItem = PathItem(
            summary = "Pet operations",
            description = "Operations for managing pets",
            get = Operation(
                summary = "List pets",
                operationId = "listPets",
                parameters = listOf(
                    Direct(Parameter(
                        name = "limit",
                        location = ParameterLocation.QUERY,
                        schema = Schema(buildJsonObject { put("type", "integer") })
                    ))
                ),
                responses = responses(
                    "200" to Response(
                        description = "List of pets",
                        content = mapOf(
                            "application/json" to MediaType(
                                schema = Schema(buildJsonObject { put("type", "array") })
                            )
                        )
                    )
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(PathItem.serializer(), pathItem)
    }

    @Test
    fun `should round-trip PathItem with multiple operations`() {
        val pathItem = PathItem(
            get = Operation(
                summary = "Get user",
                responses = responses("200" to Response(description = "User details"))
            ),
            put = Operation(
                summary = "Update user",
                responses = responses("200" to Response(description = "User updated"))
            ),
            delete = Operation(
                summary = "Delete user",
                responses = responses("204" to Response(description = "User deleted"))
            )
        )

        TestHelpers.testRoundTripWithoutValidation(PathItem.serializer(), pathItem)
    }

    // Paths Container Tests

    @Test
    fun `should create Paths with valid path patterns`() {
        val paths = paths(
            "/users" to PathItem(
                get = Operation(responses = responses("200" to Response(description = "List users")))
            ),
            "/users/{id}" to PathItem(
                get = Operation(responses = responses("200" to Response(description = "Get user")))
            ),
            "/api/v1/resources/{resourceId}/items/{itemId}" to PathItem(
                get = Operation(responses = responses("200" to Response(description = "Get item")))
            )
        )

        assertEquals(3, paths.size)
        assertTrue(paths.containsKey("/users"))
        assertTrue(paths.containsKey("/users/{id}"))
    }

    @Test
    fun `should reject path not starting with forward slash`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            paths(
                "users" to PathItem(
                    get = Operation(responses = responses("200" to Response(description = "OK")))
                )
            )
        }

        assertEquals(
            "Path 'users' must start with a forward slash (/)",
            exception.message
        )
    }

    @Test
    fun `should reject path with invalid format`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            paths(
                "api/users" to PathItem(
                    get = Operation(responses = responses("200" to Response(description = "OK")))
                )
            )
        }

        assertTrue(exception.message?.contains("must start with a forward slash") == true)
    }

    @Test
    fun `should serialize Paths map`() {
        val paths: Paths = paths(
            "/users" to PathItem(
                get = Operation(
                    summary = "List users",
                    responses = responses("200" to Response(description = "OK"))
                )
            ),
            "/users/{id}" to PathItem(
                get = Operation(
                    summary = "Get user",
                    responses = responses("200" to Response(description = "OK"))
                ),
                delete = Operation(
                    summary = "Delete user",
                    responses = responses("204" to Response(description = "Deleted"))
                )
            )
        )

        val json = compactJson.encodeToString(paths)

        assertTrue(json.contains("/users"))
        assertTrue(json.contains("/users/{id}"))
        assertTrue(json.contains("List users"))
        assertTrue(json.contains("Delete user"))
    }

    @Test
    fun `should deserialize Paths map`() {
        val json = """{"/users":{"get":{"responses":{"200":{"description":"OK"}}}}}"""
        val paths = compactJson.decodeFromString<Paths>(json)

        assertEquals(1, paths.size)
        assertTrue(paths.containsKey("/users"))
        assertEquals("OK", paths["/users"]?.get?.responses?.get("200")?.description)
    }

    @Test
    fun `should round-trip Paths map`() {
        val paths: Paths = paths(
            "/pets" to PathItem(
                get = Operation(
                    operationId = "listPets",
                    responses = responses("200" to Response(description = "List of pets"))
                ),
                post = Operation(
                    operationId = "createPet",
                    responses = responses("201" to Response(description = "Pet created"))
                )
            ),
            "/pets/{petId}" to PathItem(
                parameters = listOf(
                    Direct(Parameter(
                        name = "petId",
                        location = ParameterLocation.PATH,
                        required = true,
                        schema = Schema(buildJsonObject { put("type", "string") })
                    ))
                ),
                get = Operation(
                    operationId = "getPet",
                    responses = responses("200" to Response(description = "Pet details"))
                )
            )
        )

        val json = compactJson.encodeToString(paths)
        val deserialized = compactJson.decodeFromString<Paths>(json)

        assertEquals(paths.size, deserialized.size)
        assertEquals(paths["/pets"]?.get?.operationId, deserialized["/pets"]?.get?.operationId)
    }

    // Complex Real-world Examples

    @Test
    fun `should serialize complete REST API path item`() {
        val pathItem = PathItem(
            summary = "User resource",
            description = "Operations for user management",
            parameters = listOf(
                Direct(Parameter(
                    name = "userId",
                    location = ParameterLocation.PATH,
                    description = "User identifier",
                    required = true,
                    schema = Schema(buildJsonObject {
                        put("type", "string")
                        put("format", "uuid")
                    })
                ))
            ),
            get = Operation(
                summary = "Get user details",
                operationId = "getUser",
                tags = listOf("users"),
                responses = responses(
                    "200" to Response(
                        description = "User details retrieved",
                        content = mapOf(
                            "application/json" to MediaType(
                                schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/User") })
                            )
                        )
                    ),
                    "404" to Response(description = "User not found")
                )
            ),
            put = Operation(
                summary = "Update user",
                operationId = "updateUser",
                tags = listOf("users"),
                requestBody = RequestBody(
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/UserUpdate") })
                        )
                    ),
                    required = true
                ),
                responses = responses(
                    "200" to Response(description = "User updated"),
                    "404" to Response(description = "User not found")
                )
            ),
            delete = Operation(
                summary = "Delete user",
                operationId = "deleteUser",
                tags = listOf("users"),
                responses = responses(
                    "204" to Response(description = "User deleted"),
                    "404" to Response(description = "User not found")
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(PathItem.serializer(), pathItem)
    }
}
