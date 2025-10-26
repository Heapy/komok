package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OpenAPITest {

    // Basic OpenAPI Tests

    @Test
    fun `should serialize minimal OpenAPI with paths`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Sample API",
                version = "1.0.0"
            ),
            paths = paths(
                "/users" to PathItem(
                    get = Operation(
                        responses = responses("200" to Response(description = "Success"))
                    )
                )
            )
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("\"openapi\":\"3.2.0\""))
        assert(json.contains("\"title\":\"Sample API\""))
        assert(json.contains("/users"))
    }

    @Test
    fun `should serialize minimal OpenAPI with components`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "API",
                version = "1.0.0"
            ),
            components = Components(
                schemas = mapOf(
                    "User" to Schema(buildJsonObject { put("type", "object") })
                )
            )
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("components"))
        assert(json.contains("User"))
    }

    @Test
    fun `should serialize minimal OpenAPI with webhooks`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Webhook API",
                version = "1.0.0"
            ),
            webhooks = mapOf(
                "newPet" to PathItem(
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
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("webhooks"))
        assert(json.contains("newPet"))
    }

    // Version Pattern Tests

    @Test
    fun `should accept valid OpenAPI version 3_2_0`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
        )

        assertEquals("3.2.0", openAPI.openapi)
    }

    @Test
    fun `should accept valid OpenAPI version 3_2_1`() {
        val openAPI = OpenAPI(
            openapi = "3.2.1",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
        )

        assertEquals("3.2.1", openAPI.openapi)
    }

    @Test
    fun `should accept valid OpenAPI version with prerelease`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0-rc1",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
        )

        assertEquals("3.2.0-rc1", openAPI.openapi)
    }

    @Test
    fun `should accept valid OpenAPI version with build metadata`() {
        val openAPI = OpenAPI(
            openapi = "3.2.15-beta.2",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
        )

        assertEquals("3.2.15-beta.2", openAPI.openapi)
    }

    @Test
    fun `should reject invalid OpenAPI version 3_1_0`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OpenAPI(
                openapi = "3.1.0",
                info = Info(title = "API", version = "1.0.0"),
                paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
            )
        }

        assert(exception.message?.contains("3.1.0") == true)
        assert(exception.message?.contains("^3\\.2\\.\\d+(-.+)?\$") == true)
    }

    @Test
    fun `should reject invalid OpenAPI version 3_0_0`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OpenAPI(
                openapi = "3.0.0",
                info = Info(title = "API", version = "1.0.0"),
                paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
            )
        }

        assert(exception.message?.contains("3.0.0") == true)
    }

    @Test
    fun `should reject invalid OpenAPI version without patch`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OpenAPI(
                openapi = "3.2",
                info = Info(title = "API", version = "1.0.0"),
                paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
            )
        }

        assert(exception.message?.contains("3.2") == true)
    }

    // Document Content Validation Tests

    @Test
    fun `should reject OpenAPI without paths components or webhooks`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OpenAPI(
                openapi = "3.2.0",
                info = Info(title = "API", version = "1.0.0")
            )
        }

        assertEquals(
            "OpenAPI document must have at least one of 'paths', 'components', or 'webhooks' defined",
            exception.message
        )
    }

    @Test
    fun `should accept OpenAPI with paths and components`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            components = Components(schemas = mapOf("User" to Schema(buildJsonObject { put("type", "object") })))
        )

        assert(openAPI.paths != null)
        assert(openAPI.components != null)
    }

    // $self Validation Tests

    @Test
    fun `should accept valid self URI`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            self = "https://example.com/api/openapi.json"
        )

        assertEquals("https://example.com/api/openapi.json", openAPI.self)
    }

    @Test
    fun `should accept relative self URI`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            self = "/api/openapi.json"
        )

        assertEquals("/api/openapi.json", openAPI.self)
    }

    @Test
    fun `should reject self URI with fragment`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            OpenAPI(
                openapi = "3.2.0",
                info = Info(title = "API", version = "1.0.0"),
                paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
                self = "https://example.com/api/openapi.json#fragment"
            )
        }

        assert(exception.message?.contains("\$self") == true)
        assert(exception.message?.contains("fragment") == true)
    }

    // Servers Tests

    @Test
    fun `should serialize OpenAPI with servers`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            servers = listOf(
                Server(url = "https://api.example.com/v1", description = "Production"),
                Server(url = "https://staging-api.example.com/v1", description = "Staging")
            ),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK")))))
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("servers"))
        assert(json.contains("Production"))
        assert(json.contains("Staging"))
    }

    // Security Tests

    @Test
    fun `should serialize OpenAPI with global security`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            security = listOf(
                securityRequirement("api_key" to emptyList())
            ),
            components = Components(
                securitySchemes = mapOf(
                    "api_key" to SecurityScheme.apiKey(name = "api_key", location = ApiKeyLocation.HEADER)
                )
            )
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("security"))
        assert(json.contains("api_key"))
    }

    // Tags Tests

    @Test
    fun `should serialize OpenAPI with tags`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            tags = listOf(
                Tag(name = "users", description = "User operations"),
                Tag(name = "pets", description = "Pet operations")
            )
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("tags"))
        assert(json.contains("users"))
        assert(json.contains("pets"))
    }

    // External Docs Tests

    @Test
    fun `should serialize OpenAPI with externalDocs`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            externalDocs = ExternalDocumentation(
                description = "Find more info here",
                url = "https://example.com/docs"
            )
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("externalDocs"))
        assert(json.contains("Find more info here"))
    }

    // JSON Schema Dialect Tests

    @Test
    fun `should serialize OpenAPI with custom jsonSchemaDialect`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            jsonSchemaDialect = "https://json-schema.org/draft/2020-12/schema"
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("jsonSchemaDialect"))
        assert(json.contains("json-schema.org"))
    }

    // Specification Extensions Tests

    @Test
    fun `should serialize OpenAPI with specification extensions`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "API", version = "1.0.0"),
            paths = paths("/" to PathItem(get = Operation(responses = responses("200" to Response(description = "OK"))))),
            extensions = mapOf(
                "x-api-id" to JsonPrimitive("12345"),
                "x-internal" to JsonPrimitive(true)
            )
        )
        val json = compactJson.encodeToString(openAPI)

        assert(json.contains("x-api-id"))
        assert(json.contains("x-internal"))
    }

    // Deserialization Tests

    @Test
    fun `should deserialize OpenAPI document`() {
        val json = """{"openapi":"3.2.0","info":{"title":"Test","version":"1.0.0"},"paths":{"/test":{"get":{"responses":{"200":{"description":"OK"}}}}}}"""
        val openAPI = compactJson.decodeFromString<OpenAPI>(json)

        assertEquals("3.2.0", openAPI.openapi)
        assertEquals("Test", openAPI.info.title)
        assert(openAPI.paths?.containsKey("/test") == true)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip minimal OpenAPI document`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Sample API",
                version = "1.0.0"
            ),
            paths = paths(
                "/" to PathItem(
                    get = Operation(
                        responses = responses("200" to Response(description = "OK"))
                    )
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), openAPI)
    }

    @Test
    fun `should round-trip complete OpenAPI document`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Complete API",
                version = "1.0.0",
                description = "A complete API example",
                contact = Contact(name = "API Team", email = "api@example.com"),
                license = License(name = "MIT", identifier = "MIT")
            ),
            servers = listOf(
                Server(url = "https://api.example.com")
            ),
            paths = paths(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List users",
                        responses = responses("200" to Response(description = "Success"))
                    )
                )
            ),
            components = Components(
                schemas = mapOf(
                    "User" to Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                ),
                securitySchemes = mapOf(
                    "api_key" to SecurityScheme.apiKey(name = "api_key", location = ApiKeyLocation.HEADER)
                )
            ),
            security = listOf(
                securityRequirement("api_key" to emptyList())
            ),
            tags = listOf(
                Tag(name = "users", description = "User operations")
            )
        )

        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), openAPI)
    }

    // Real-world Example

    @Test
    fun `should serialize Petstore OpenAPI document`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Swagger Petstore - OpenAPI 3.2",
                version = "1.0.11",
                description = "This is a sample Pet Store Server based on the OpenAPI 3.2 specification.",
                termsOfService = "http://swagger.io/terms/",
                contact = Contact(email = "apiteam@swagger.io"),
                license = License(
                    name = "Apache 2.0",
                    url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                )
            ),
            externalDocs = ExternalDocumentation(
                description = "Find out more about Swagger",
                url = "http://swagger.io"
            ),
            servers = listOf(
                Server(url = "/api/v3")
            ),
            tags = listOf(
                Tag(name = "pet", description = "Everything about your Pets"),
                Tag(name = "store", description = "Access to Petstore orders"),
                Tag(name = "user", description = "Operations about user")
            ),
            paths = paths(
                "/pet" to PathItem(
                    post = Operation(
                        tags = listOf("pet"),
                        summary = "Add a new pet to the store",
                        operationId = "addPet",
                        requestBody = RequestBody(
                            description = "Create a new pet in the store",
                            required = true,
                            content = mapOf(
                                "application/json" to MediaType(
                                    schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Pet") })
                                )
                            )
                        ),
                        responses = responses(
                            "200" to Response(
                                description = "Successful operation",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Pet") })
                                    )
                                )
                            ),
                            "405" to Response(description = "Invalid input")
                        )
                    )
                )
            ),
            components = Components(
                schemas = mapOf(
                    "Pet" to Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("name", buildJsonObject { put("type", "string") })
                            put("status", buildJsonObject {
                                put("type", "string")
                                put("enum", buildJsonObject {
                                    put("0", "available")
                                    put("1", "pending")
                                    put("2", "sold")
                                })
                            })
                        })
                    })
                ),
                securitySchemes = mapOf(
                    "petstore_auth" to SecurityScheme.oauth2(
                        flows = OAuthFlows(
                            implicit = OAuthFlow.Implicit(
                                authorizationUrl = "https://petstore3.swagger.io/oauth/authorize",
                                scopes = mapOf(
                                    "write:pets" to "modify pets in your account",
                                    "read:pets" to "read your pets"
                                )
                            )
                        )
                    ),
                    "api_key" to SecurityScheme.apiKey(
                        name = "api_key",
                        location = ApiKeyLocation.HEADER
                    )
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), openAPI)
    }

    // Constants Tests

    @Test
    fun `should have correct VERSION constant`() {
        assertEquals("3.2.0", OpenAPI.VERSION_3_2_0)
    }

    @Test
    fun `should have correct DEFAULT_JSON_SCHEMA_DIALECT constant`() {
        assertEquals("https://spec.openapis.org/oas/3.2/dialect/2025-09-17", OpenAPI.DEFAULT_JSON_SCHEMA_DIALECT)
    }
}
