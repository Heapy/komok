package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ComponentsTest {

    // Basic Components Tests

    @Test
    fun `should serialize empty Components`() {
        val components = Components()
        val json = compactJson.encodeToString(components)

        val expected = "{}"
        assertEquals(expected, json)
    }

    // Schemas Tests

    @Test
    fun `should serialize Components with schemas`() {
        val components = Components(
            schemas = mapOf(
                "User" to Schema(buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("id", buildJsonObject { put("type", "integer") })
                        put("username", buildJsonObject { put("type", "string") })
                    })
                }),
                "Error" to Schema(buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("code", buildJsonObject { put("type", "integer") })
                        put("message", buildJsonObject { put("type", "string") })
                    })
                })
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("schemas"))
        assert(json.contains("User"))
        assert(json.contains("Error"))
    }

    // Responses Tests

    @Test
    fun `should serialize Components with responses`() {
        val components = Components(
            responses = mapOf(
                "NotFound" to Response(
                    description = "Entity not found"
                ),
                "IllegalInput" to Response(
                    description = "Illegal input for operation"
                ),
                "GeneralError" to Response(
                    description = "General error",
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Error") })
                        )
                    )
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("responses"))
        assert(json.contains("NotFound"))
        assert(json.contains("IllegalInput"))
        assert(json.contains("GeneralError"))
    }

    // Parameters Tests

    @Test
    fun `should serialize Components with parameters`() {
        val components = Components(
            parameters = mapOf(
                "skipParam" to Parameter(
                    name = "skip",
                    location = ParameterLocation.QUERY,
                    description = "Number of items to skip",
                    schema = Schema(buildJsonObject {
                        put("type", "integer")
                        put("format", "int32")
                    })
                ),
                "limitParam" to Parameter(
                    name = "limit",
                    location = ParameterLocation.QUERY,
                    description = "Max number of items to return",
                    schema = Schema(buildJsonObject {
                        put("type", "integer")
                        put("format", "int32")
                    })
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("parameters"))
        assert(json.contains("skipParam"))
        assert(json.contains("limitParam"))
    }

    // Examples Tests

    @Test
    fun `should serialize Components with examples`() {
        val components = Components(
            examples = mapOf(
                "userExample" to Example(
                    summary = "User example",
                    value = buildJsonObject {
                        put("id", 1)
                        put("username", "johndoe")
                    }
                ),
                "errorExample" to Example(
                    summary = "Error example",
                    value = buildJsonObject {
                        put("code", 404)
                        put("message", "Not found")
                    }
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("examples"))
        assert(json.contains("userExample"))
        assert(json.contains("errorExample"))
    }

    // Request Bodies Tests

    @Test
    fun `should serialize Components with requestBodies`() {
        val components = Components(
            requestBodies = mapOf(
                "UserArray" to RequestBody(
                    description = "List of user objects",
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject {
                                put("type", "array")
                                put("items", buildJsonObject { put("\$ref", "#/components/schemas/User") })
                            })
                        )
                    )
                ),
                "Pet" to RequestBody(
                    description = "Pet object to be added",
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Pet") })
                        )
                    ),
                    required = true
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("requestBodies"))
        assert(json.contains("UserArray"))
        assert(json.contains("Pet"))
    }

    // Headers Tests

    @Test
    fun `should serialize Components with headers`() {
        val components = Components(
            headers = mapOf(
                "X-Rate-Limit-Limit" to Header(
                    description = "The number of allowed requests in the current period",
                    schema = Schema(buildJsonObject { put("type", "integer") })
                ),
                "X-Rate-Limit-Remaining" to Header(
                    description = "The number of remaining requests in the current period",
                    schema = Schema(buildJsonObject { put("type", "integer") })
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("headers"))
        assert(json.contains("X-Rate-Limit-Limit"))
        assert(json.contains("X-Rate-Limit-Remaining"))
    }

    // Security Schemes Tests

    @Test
    fun `should serialize Components with securitySchemes`() {
        val components = Components(
            securitySchemes = mapOf(
                "api_key" to SecurityScheme.apiKey(
                    name = "api_key",
                    location = ApiKeyLocation.HEADER
                ),
                "petstore_auth" to SecurityScheme.oauth2(
                    flows = OAuthFlows(
                        implicit = OAuthFlow.Implicit(
                            authorizationUrl = "https://petstore.swagger.io/oauth/authorize",
                            scopes = mapOf(
                                "write:pets" to "Modify pets",
                                "read:pets" to "Read pets"
                            )
                        )
                    )
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("securitySchemes"))
        assert(json.contains("api_key"))
        assert(json.contains("petstore_auth"))
    }

    // Links Tests

    @Test
    fun `should serialize Components with links`() {
        val components = Components(
            links = mapOf(
                "UserRepositories" to Link(
                    operationId = "getRepositoriesByOwner",
                    parameters = mapOf(
                        "username" to "\$response.body#/username"
                    )
                ),
                "UserByUserId" to Link(
                    operationId = "getUser",
                    parameters = mapOf(
                        "userId" to "\$response.body#/userId"
                    )
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("links"))
        assert(json.contains("UserRepositories"))
        assert(json.contains("UserByUserId"))
    }

    // Callbacks Tests

    @Test
    fun `should serialize Components with callbacks`() {
        val components = Components(
            callbacks = mapOf(
                "onData" to mapOf(
                    "{${'$'}request.body#/callbackUrl}/data" to PathItem(
                        post = Operation(
                            requestBody = RequestBody(
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("type", "object") })
                                    )
                                )
                            ),
                            responses = responses("200" to Response(description = "OK"))
                        )
                    )
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("callbacks"))
        assert(json.contains("onData"))
    }

    // Path Items Tests

    @Test
    fun `should serialize Components with pathItems`() {
        val components = Components(
            pathItems = mapOf(
                "commonPath" to PathItem(
                    get = Operation(
                        responses = responses("200" to Response(description = "OK"))
                    )
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("pathItems"))
        assert(json.contains("commonPath"))
    }

    // Media Types Tests

    @Test
    fun `should serialize Components with mediaTypes`() {
        val components = Components(
            mediaTypes = mapOf(
                "jsonMediaType" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                )
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("mediaTypes"))
        assert(json.contains("jsonMediaType"))
    }

    // Component Naming Pattern Tests

    @Test
    fun `should accept valid component names`() {
        val components = Components(
            schemas = mapOf(
                "User" to Schema(buildJsonObject { put("type", "object") }),
                "User_V2" to Schema(buildJsonObject { put("type", "object") }),
                "user.model" to Schema(buildJsonObject { put("type", "object") }),
                "user-model" to Schema(buildJsonObject { put("type", "object") }),
                "User123" to Schema(buildJsonObject { put("type", "object") }),
                "ABC_DEF.GHI-JKL" to Schema(buildJsonObject { put("type", "object") })
            )
        )

        // Should not throw exception
        val json = compactJson.encodeToString(components)
        assert(json.contains("User"))
    }

    @Test
    fun `should reject component name with spaces`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Components(
                schemas = mapOf(
                    "User Model" to Schema(buildJsonObject { put("type", "object") })
                )
            )
        }

        assert(exception.message?.contains("User Model") == true)
        assert(exception.message?.contains("schemas") == true)
        assert(exception.message?.contains("^[a-zA-Z0-9._-]+$") == true)
    }

    @Test
    fun `should reject component name with special characters`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Components(
                parameters = mapOf(
                    "user@param" to Parameter(
                        name = "user",
                        location = ParameterLocation.QUERY,
                        schema = Schema(buildJsonObject { put("type", "string") })
                    )
                )
            )
        }

        assert(exception.message?.contains("user@param") == true)
        assert(exception.message?.contains("parameters") == true)
    }

    @Test
    fun `should reject component name starting with special character`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Components(
                examples = mapOf(
                    "#example" to Example(value = JsonPrimitive("test"))
                )
            )
        }

        assert(exception.message?.contains("#example") == true)
    }

    @Test
    fun `should reject empty component name`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Components(
                responses = mapOf(
                    "" to Response(description = "Empty name")
                )
            )
        }

        assert(exception.message?.contains("responses") == true)
    }

    // Specification Extensions Tests

    @Test
    fun `should serialize Components with specification extensions`() {
        val components = Components(
            schemas = mapOf(
                "User" to Schema(buildJsonObject { put("type", "object") })
            ),
            extensions = mapOf(
                "x-internal-id" to JsonPrimitive("comp-123")
            )
        )
        val json = compactJson.encodeToString(components)

        assert(json.contains("x-internal-id"))
        assert(json.contains("comp-123"))
    }

    // Deserialization Tests

    @Test
    fun `should deserialize Components with schemas`() {
        val json = """{"schemas":{"User":{"type":"object"}}}"""
        val components = compactJson.decodeFromString<Components>(json)

        assertEquals(1, components.schemas?.size)
        assert(components.schemas?.containsKey("User") == true)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip Components with single component type`() {
        val components = Components(
            schemas = mapOf(
                "Pet" to Schema(buildJsonObject {
                    put("type", "object")
                    put("required", buildJsonObject {
                        put("0", "name")
                    })
                    put("properties", buildJsonObject {
                        put("name", buildJsonObject { put("type", "string") })
                        put("tag", buildJsonObject { put("type", "string") })
                    })
                })
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Components.serializer(), components)
    }

    @Test
    fun `should round-trip Components with multiple component types`() {
        val components = Components(
            schemas = mapOf(
                "User" to Schema(buildJsonObject { put("type", "object") })
            ),
            responses = mapOf(
                "NotFound" to Response(description = "Not found")
            ),
            parameters = mapOf(
                "limit" to Parameter(
                    name = "limit",
                    location = ParameterLocation.QUERY,
                    schema = Schema(buildJsonObject { put("type", "integer") })
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Components.serializer(), components)
    }

    // Real-world Complete Example

    @Test
    fun `should serialize complete Components object`() {
        val components = Components(
            schemas = mapOf(
                "Pet" to Schema(buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("id", buildJsonObject { put("type", "integer") })
                        put("name", buildJsonObject { put("type", "string") })
                    })
                }),
                "Error" to Schema(buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("code", buildJsonObject { put("type", "integer") })
                        put("message", buildJsonObject { put("type", "string") })
                    })
                })
            ),
            responses = mapOf(
                "NotFound" to Response(description = "Entity not found"),
                "IllegalInput" to Response(description = "Illegal input")
            ),
            parameters = mapOf(
                "skipParam" to Parameter(
                    name = "skip",
                    location = ParameterLocation.QUERY,
                    schema = Schema(buildJsonObject { put("type", "integer") })
                ),
                "limitParam" to Parameter(
                    name = "limit",
                    location = ParameterLocation.QUERY,
                    schema = Schema(buildJsonObject { put("type", "integer") })
                )
            ),
            securitySchemes = mapOf(
                "api_key" to SecurityScheme.apiKey(
                    name = "api_key",
                    location = ApiKeyLocation.HEADER
                ),
                "petstore_auth" to SecurityScheme.oauth2(
                    flows = OAuthFlows(
                        implicit = OAuthFlow.Implicit(
                            authorizationUrl = "https://example.com/api/oauth/dialog",
                            scopes = mapOf(
                                "write:pets" to "modify pets in your account",
                                "read:pets" to "read your pets"
                            )
                        )
                    )
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Components.serializer(), components)
    }
}
