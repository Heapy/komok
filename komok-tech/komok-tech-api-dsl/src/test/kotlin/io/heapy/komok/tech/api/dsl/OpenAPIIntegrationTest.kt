package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Integration tests for the complete OpenAPI model.
 * These tests verify that all components work together correctly and produce valid OpenAPI 3.2 documents.
 */
class OpenAPIIntegrationTest {

    @Test
    fun `should create complete valid OpenAPI 3_2 document`() {
        // Create a complete OpenAPI document using all major components
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Pet Store API",
                version = "1.0.0",
                summary = "A sample pet store server",
                description = "This is a sample server for a pet store.",
                termsOfService = "http://example.com/terms/",
                contact = Contact(
                    name = "API Support",
                    url = "http://www.example.com/support",
                    email = "support@example.com"
                ),
                license = License(
                    name = "Apache 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
            ),
            servers = listOf(
                Server(
                    url = "https://petstore.swagger.io/v2",
                    description = "Production server"
                ),
                Server(
                    url = "https://staging.swagger.io/v2",
                    description = "Staging server"
                )
            ),
            tags = listOf(
                Tag(
                    name = "pet",
                    description = "Everything about your Pets",
                    externalDocs = ExternalDocumentation(
                        description = "Find out more",
                        url = "http://swagger.io"
                    )
                ),
                Tag(name = "store", description = "Access to Petstore orders"),
                Tag(name = "user", description = "Operations about user")
            ),
            paths = paths(
                "/pets" to PathItem(
                    summary = "Pet operations",
                    get = Operation(
                        tags = listOf("pet"),
                        summary = "List all pets",
                        operationId = "listPets",
                        parameters = listOf(
                            Parameter(
                                name = "limit",
                                location = ParameterLocation.QUERY,
                                description = "How many items to return at one time (max 100)",
                                required = false,
                                schema = Schema(buildJsonObject {
                                    put("type", "integer")
                                    put("format", "int32")
                                })
                            )
                        ),
                        responses = responses(
                            "200" to Response(
                                description = "A paged array of pets",
                                headers = mapOf(
                                    "x-next" to Header(
                                        description = "A link to the next page of responses",
                                        schema = Schema(buildJsonObject { put("type", "string") })
                                    )
                                ),
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Pets") })
                                    )
                                )
                            ),
                            "default" to Response(
                                description = "Unexpected error",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Error") })
                                    )
                                )
                            )
                        )
                    ),
                    post = Operation(
                        tags = listOf("pet"),
                        summary = "Create a pet",
                        operationId = "createPets",
                        requestBody = RequestBody(
                            description = "Pet to add to the store",
                            required = true,
                            content = mapOf(
                                "application/json" to MediaType(
                                    schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Pet") })
                                )
                            )
                        ),
                        responses = responses(
                            "201" to Response(description = "Null response"),
                            "default" to Response(
                                description = "Unexpected error",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Error") })
                                    )
                                )
                            )
                        ),
                        security = listOf(
                            securityRequirement("petstore_auth" to listOf("write:pets", "read:pets"))
                        )
                    )
                ),
                "/pets/{petId}" to PathItem(
                    summary = "Individual pet operations",
                    parameters = listOf(
                        Parameter(
                            name = "petId",
                            location = ParameterLocation.PATH,
                            description = "The id of the pet to retrieve",
                            required = true,
                            schema = Schema(buildJsonObject { put("type", "string") })
                        )
                    ),
                    get = Operation(
                        tags = listOf("pet"),
                        summary = "Info for a specific pet",
                        operationId = "showPetById",
                        responses = responses(
                            "200" to Response(
                                description = "Expected response to a valid request",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Pet") })
                                    )
                                )
                            ),
                            "default" to Response(
                                description = "Unexpected error",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Error") })
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            components = Components(
                schemas = mapOf(
                    "Pet" to Schema(buildJsonObject {
                        put("type", "object")
                        put("required", buildJsonObject {
                            put("0", "id")
                            put("1", "name")
                        })
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject {
                                put("type", "integer")
                                put("format", "int64")
                            })
                            put("name", buildJsonObject { put("type", "string") })
                            put("tag", buildJsonObject { put("type", "string") })
                        })
                    }),
                    "Pets" to Schema(buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("\$ref", "#/components/schemas/Pet") })
                    }),
                    "Error" to Schema(buildJsonObject {
                        put("type", "object")
                        put("required", buildJsonObject {
                            put("0", "code")
                            put("1", "message")
                        })
                        put("properties", buildJsonObject {
                            put("code", buildJsonObject {
                                put("type", "integer")
                                put("format", "int32")
                            })
                            put("message", buildJsonObject { put("type", "string") })
                        })
                    })
                ),
                securitySchemes = mapOf(
                    "petstore_auth" to SecurityScheme.oauth2(
                        description = "Get access to data while protecting your account credentials",
                        flows = OAuthFlows(
                            implicit = OAuthFlow.Implicit(
                                authorizationUrl = "http://petstore.swagger.io/api/oauth/dialog",
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
                ),
                responses = mapOf(
                    "UnexpectedError" to Response(
                        description = "Unexpected error",
                        content = mapOf(
                            "application/json" to MediaType(
                                schema = Schema(buildJsonObject { put("\$ref", "#/components/schemas/Error") })
                            )
                        )
                    )
                ),
                parameters = mapOf(
                    "limitParam" to Parameter(
                        name = "limit",
                        location = ParameterLocation.QUERY,
                        description = "Maximum number of results to return",
                        schema = Schema(buildJsonObject {
                            put("type", "integer")
                            put("format", "int32")
                        })
                    )
                )
            ),
            security = listOf(
                securityRequirement("api_key" to emptyList())
            ),
            externalDocs = ExternalDocumentation(
                description = "Find out more about Swagger",
                url = "http://swagger.io"
            )
        )

        // Serialize to JSON
        val json = compactJson.encodeToString(openAPI)

        // Verify JSON is not empty
        assertNotNull(json)
        assert(json.isNotEmpty())

        // Verify it contains expected structure
        assert(json.contains("\"openapi\":\"3.2.0\""))
        assert(json.contains("\"title\":\"Pet Store API\""))
        assert(json.contains("\"/pets\""))
        assert(json.contains("\"/pets/{petId}\""))
        assert(json.contains("\"components\""))
        assert(json.contains("\"Pet\""))
        assert(json.contains("\"securitySchemes\""))

        // Deserialize back
        val deserialized = compactJson.decodeFromString<OpenAPI>(json)

        // Verify round-trip
        assertEquals("3.2.0", deserialized.openapi)
        assertEquals("Pet Store API", deserialized.info.title)
        assertEquals("1.0.0", deserialized.info.version)
        assertEquals(2, deserialized.servers?.size)
        assertEquals(3, deserialized.tags?.size)
        assertEquals(2, deserialized.paths?.size)
        assertNotNull(deserialized.components)
        assertEquals(3, deserialized.components?.schemas?.size)
        assertEquals(2, deserialized.components?.securitySchemes?.size)

        // Validate against JSON Schema
        OpenAPISchemaValidator.validate(json)
    }

    @Test
    fun `should handle minimal valid OpenAPI document`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Minimal API",
                version = "1.0.0"
            ),
            paths = paths(
                "/" to PathItem(
                    get = Operation(
                        responses = responses(
                            "200" to Response(description = "OK")
                        )
                    )
                )
            )
        )

        val json = compactJson.encodeToString(openAPI)

        // Validate against JSON Schema
        OpenAPISchemaValidator.validate(json)

        // Verify round-trip
        val deserialized = compactJson.decodeFromString<OpenAPI>(json)
        assertEquals("3.2.0", deserialized.openapi)
        assertEquals("Minimal API", deserialized.info.title)
    }

    @Test
    fun `should handle OpenAPI document with webhooks`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Webhook API",
                version = "1.0.0"
            ),
            webhooks = mapOf(
                "newPet" to PathItem(
                    post = Operation(
                        summary = "New pet webhook",
                        description = "Triggered when a new pet is added",
                        requestBody = RequestBody(
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
                        ),
                        responses = responses(
                            "200" to Response(description = "Return a 200 status to indicate that the data was received successfully")
                        )
                    )
                )
            )
        )

        val json = compactJson.encodeToString(openAPI)

        // Validate against JSON Schema
        OpenAPISchemaValidator.validate(json)

        // Verify round-trip
        val deserialized = compactJson.decodeFromString<OpenAPI>(json)
        assertEquals(1, deserialized.webhooks?.size)
        assertNotNull(deserialized.webhooks?.get("newPet"))
    }

    @Test
    fun `should handle OpenAPI document with components only`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Component Library API",
                version = "1.0.0",
                description = "A library of reusable components"
            ),
            components = Components(
                schemas = mapOf(
                    "User" to Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("username", buildJsonObject { put("type", "string") })
                            put("email", buildJsonObject {
                                put("type", "string")
                                put("format", "email")
                            })
                        })
                    }),
                    "Product" to Schema(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("id", buildJsonObject { put("type", "integer") })
                            put("name", buildJsonObject { put("type", "string") })
                            put("price", buildJsonObject {
                                put("type", "number")
                                put("format", "double")
                            })
                        })
                    })
                ),
                responses = mapOf(
                    "NotFound" to Response(description = "The specified resource was not found"),
                    "Unauthorized" to Response(description = "Unauthorized")
                ),
                parameters = mapOf(
                    "PageParam" to Parameter(
                        name = "page",
                        location = ParameterLocation.QUERY,
                        description = "Page number",
                        schema = Schema(buildJsonObject { put("type", "integer") })
                    )
                ),
                securitySchemes = mapOf(
                    "bearerAuth" to SecurityScheme.http(
                        scheme = "bearer",
                        bearerFormat = "JWT"
                    )
                )
            )
        )

        val json = compactJson.encodeToString(openAPI)

        // Validate against JSON Schema
        OpenAPISchemaValidator.validate(json)

        // Verify structure
        val deserialized = compactJson.decodeFromString<OpenAPI>(json)
        assertEquals(2, deserialized.components?.schemas?.size)
        assertEquals(2, deserialized.components?.responses?.size)
        assertEquals(1, deserialized.components?.parameters?.size)
        assertEquals(1, deserialized.components?.securitySchemes?.size)
    }

    @Test
    fun `should serialize and validate complex document with all features`() {
        val openAPI = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Comprehensive API",
                version = "2.5.0",
                summary = "A comprehensive API demonstrating all OpenAPI 3.2 features",
                description = "This API showcases the complete OpenAPI 3.2 specification",
                termsOfService = "https://example.com/terms",
                contact = Contact(
                    name = "API Team",
                    email = "api@example.com",
                    url = "https://example.com/support"
                ),
                license = License(
                    name = "MIT",
                    identifier = "MIT"
                )
            ),
            jsonSchemaDialect = "https://json-schema.org/draft/2020-12/schema",
            servers = listOf(
                Server(
                    url = "https://{environment}.example.com:{port}/api",
                    description = "Main API server",
                    variables = mapOf(
                        "environment" to ServerVariable(
                            default = "api",
                            enum = listOf("api", "staging", "dev"),
                            description = "Server environment"
                        ),
                        "port" to ServerVariable(
                            default = "443",
                            description = "Server port"
                        )
                    )
                )
            ),
            paths = paths(
                "/users" to PathItem(
                    get = Operation(
                        operationId = "listUsers",
                        summary = "List users",
                        tags = listOf("users"),
                        parameters = listOf(
                            Parameter(
                                name = "limit",
                                location = ParameterLocation.QUERY,
                                schema = Schema(buildJsonObject { put("type", "integer") })
                            )
                        ),
                        responses = responses(
                            "200" to Response(
                                description = "Success",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(buildJsonObject {
                                            put("type", "array")
                                            put("items", buildJsonObject { put("\$ref", "#/components/schemas/User") })
                                        })
                                    )
                                )
                            )
                        )
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
                    "oauth2" to SecurityScheme.oauth2(
                        flows = OAuthFlows(
                            authorizationCode = OAuthFlow.AuthorizationCode(
                                authorizationUrl = "https://example.com/oauth/authorize",
                                tokenUrl = "https://example.com/oauth/token",
                                scopes = mapOf(
                                    "read" to "Read access",
                                    "write" to "Write access"
                                )
                            )
                        )
                    )
                )
            ),
            security = listOf(
                securityRequirement("oauth2" to listOf("read"))
            ),
            tags = listOf(
                Tag(name = "users", description = "User operations")
            ),
            externalDocs = ExternalDocumentation(
                description = "API Documentation",
                url = "https://example.com/docs"
            )
        )

        val json = compactJson.encodeToString(openAPI)

        // Validate against JSON Schema
        OpenAPISchemaValidator.validate(json)

        // Print for manual inspection (optional)
        println("Generated OpenAPI Document:")
        println(json)

        // Verify deserialization
        val deserialized = compactJson.decodeFromString<OpenAPI>(json)
        assertEquals("3.2.0", deserialized.openapi)
        assertEquals("Comprehensive API", deserialized.info.title)
    }

    @Test
    fun `should parse and validate real-world Petstore OpenAPI document`() {
        // Load the Petstore 3.0.0 JSON file from resources
        val petstoreJson = this::class.java.classLoader
            .getResourceAsStream("petstore-full-3.0.0.json")
            ?.bufferedReader()
            ?.readText()
            ?: error("Could not load petstore-full-3.0.0.json from resources")

        // Parse as JsonElement to modify the version
        val jsonElement = compactJson.parseToJsonElement(petstoreJson)
        require(jsonElement is kotlinx.serialization.json.JsonObject) {
            "Expected JSON object at root"
        }

        // Convert version from 3.0.0 to 3.2.0 (our model only supports 3.2.x)
        val modifiedJson = kotlinx.serialization.json.JsonObject(
            jsonElement.toMutableMap().apply {
                put("openapi", kotlinx.serialization.json.JsonPrimitive("3.2.0"))
            }
        )

        // Deserialize to our OpenAPI model
        val openAPI = compactJson.decodeFromJsonElement(OpenAPI.serializer(), modifiedJson)

        // Verify basic structure
        assertEquals("3.2.0", openAPI.openapi)
        assertEquals("Swagger Petstore - OpenAPI 3.0", openAPI.info.title)
        assertEquals("1.0.27", openAPI.info.version)
        assertNotNull(openAPI.paths)
        assertNotNull(openAPI.components)

        // Verify paths are parsed
        val paths = openAPI.paths
        assertNotNull(paths)
        assert(paths!!.containsKey("/pet"))
        assert(paths.containsKey("/pet/findByStatus"))
        assert(paths.containsKey("/pet/{petId}"))
        assert(paths.containsKey("/user"))

        // Verify operations are parsed
        val petPath = paths["/pet"]
        assertNotNull(petPath)
        assertNotNull(petPath!!.put)
        assertNotNull(petPath.post)
        assertEquals("updatePet", petPath.put?.operationId)
        assertEquals("addPet", petPath.post?.operationId)

        // Verify components are parsed
        val components = openAPI.components
        assertNotNull(components)
        assertNotNull(components!!.schemas)
        assert(components.schemas!!.containsKey("Pet"))
        assert(components.schemas!!.containsKey("Order"))
        assert(components.schemas!!.containsKey("User"))

        // Verify security schemes
        assertNotNull(components.securitySchemes)
        assert(components.securitySchemes!!.containsKey("petstore_auth"))
        assert(components.securitySchemes!!.containsKey("api_key"))

        // Serialize back to JSON
        val serialized = compactJson.encodeToString(openAPI)

        // Validate against OpenAPI 3.2 JSON Schema
        OpenAPISchemaValidator.validate(serialized)

        // Verify round-trip: deserialize the serialized JSON and compare full object
        val roundTrip = compactJson.decodeFromString<OpenAPI>(serialized)
        assertEquals(openAPI, roundTrip, "Round-trip serialization should preserve all data")

        println("Successfully parsed, serialized, and validated Petstore OpenAPI document")
        println("Paths: ${paths.size}")
        println("Schemas: ${components.schemas!!.size}")
        println("Security Schemes: ${components.securitySchemes!!.size}")
    }
}
