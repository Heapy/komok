package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComponentsAndRootDslTest {

    // ============================================
    // Link DSL Tests
    // ============================================

    @Test
    fun `link DSL should create link with operationId`() {
        val result = link {
            operationId = "getUser"
            description = "Get the user"
        }

        assertEquals(
            Link(
                operationId = "getUser",
                description = "Get the user"
            ),
            result
        )
    }

    @Test
    fun `link DSL should create link with operationRef`() {
        val result = link {
            operationRef = "#/paths/~1users~1{userId}/get"
            description = "Get user by ref"
        }

        assertEquals(
            Link(
                operationRef = "#/paths/~1users~1{userId}/get",
                description = "Get user by ref"
            ),
            result
        )
    }

    @Test
    fun `link DSL should create link with all fields`() {
        val result = link {
            operationId = "getUser"
            parameters = mapOf("userId" to "\$response.body#/id")
            requestBody = JsonPrimitive("\$response.body")
            description = "Get the user that owns this resource"
            server {
                url = "https://api.example.com"
            }
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertEquals(
            Link(
                operationId = "getUser",
                parameters = mapOf("userId" to "\$response.body#/id"),
                requestBody = JsonPrimitive("\$response.body"),
                description = "Get the user that owns this resource",
                server = Server(url = "https://api.example.com"),
                extensions = mapOf("x-custom" to JsonPrimitive("value"))
            ),
            result
        )
    }

    @Test
    fun `link DSL should fail when neither operationRef nor operationId is provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            link {
                description = "Missing operation"
            }
        }

        assertEquals(
            "Link must have exactly one of 'operationRef' or 'operationId' specified",
            exception.message
        )
    }

    @Test
    fun `link DSL should fail when both operationRef and operationId are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            link {
                operationRef = "#/paths/~1users/get"
                operationId = "getUsers"
            }
        }

        assertEquals(
            "Link must have exactly one of 'operationRef' or 'operationId' specified",
            exception.message
        )
    }

    @Test
    fun `link DSL should serialize correctly`() {
        val result = link {
            operationId = "getUser"
            parameters = mapOf("userId" to "\$response.body#/id")
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"operationId":"getUser","parameters":{"userId":"${'$'}response.body#/id"}}""",
            json
        )
    }

    @Test
    fun `link DSL should round-trip correctly`() {
        val result = link {
            operationId = "getUser"
            parameters = mapOf("userId" to "\$response.body#/id")
            description = "Get user"
        }

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), result)
    }

    // ============================================
    // Info DSL Tests
    // ============================================

    @Test
    fun `info DSL should create info with required fields`() {
        val result = info {
            title = "My API"
            version = "1.0.0"
        }

        assertEquals(
            Info(title = "My API", version = "1.0.0"),
            result
        )
    }

    @Test
    fun `info DSL should create info with all fields`() {
        val result = info {
            title = "Petstore API"
            version = "1.0.0"
            summary = "A sample Petstore API"
            description = "A sample API that uses a petstore"
            termsOfService = "https://example.com/terms/"
            contact {
                name = "API Support"
                url = "https://example.com/support"
                email = "support@example.com"
            }
            license {
                name = "Apache 2.0"
                identifier = "Apache-2.0"
            }
            extensions = mapOf("x-api-id" to JsonPrimitive("petstore-001"))
        }

        assertEquals(
            Info(
                title = "Petstore API",
                version = "1.0.0",
                summary = "A sample Petstore API",
                description = "A sample API that uses a petstore",
                termsOfService = "https://example.com/terms/",
                contact = Contact(
                    name = "API Support",
                    url = "https://example.com/support",
                    email = "support@example.com"
                ),
                license = License(
                    name = "Apache 2.0",
                    identifier = "Apache-2.0"
                ),
                extensions = mapOf("x-api-id" to JsonPrimitive("petstore-001"))
            ),
            result
        )
    }

    @Test
    fun `info DSL should fail when title is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            info {
                version = "1.0.0"
            }
        }

        assertEquals("Info title is required", exception.message)
    }

    @Test
    fun `info DSL should fail when version is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            info {
                title = "My API"
            }
        }

        assertEquals("Info version is required", exception.message)
    }

    @Test
    fun `info DSL should serialize correctly`() {
        val result = info {
            title = "My API"
            version = "1.0.0"
            summary = "A sample API"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"title":"My API","version":"1.0.0","summary":"A sample API"}""",
            json
        )
    }

    @Test
    fun `info DSL should round-trip correctly`() {
        val result = info {
            title = "Petstore"
            version = "2.0.0"
            contact {
                name = "Support"
                email = "support@example.com"
            }
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Info.serializer(), result)
    }

    // ============================================
    // Components DSL Tests
    // ============================================

    @Test
    fun `components DSL should create empty components`() {
        val result = components {}

        assertEquals(Components(), result)
    }

    @Test
    fun `components DSL should create components with schemas`() {
        val result = components {
            schemas {
                "Pet" to {
                    type = "object"
                    required("name")
                    properties {
                        "name" to stringSchema()
                        "tag" to stringSchema()
                    }
                }
            }
        }

        assertNotNull(result.schemas)
        assertEquals(1, result.schemas!!.size)
        assertTrue(result.schemas!!.containsKey("Pet"))
    }

    @Test
    fun `components DSL should create components with responses`() {
        val result = components {
            responses {
                "NotFound" to {
                    description = "Resource not found"
                }
                "Unauthorized" to {
                    description = "Authentication required"
                }
            }
        }

        assertNotNull(result.responses)
        assertEquals(2, result.responses!!.size)
        assertEquals("Resource not found", result.responses!!["NotFound"]?.description)
        assertEquals("Authentication required", result.responses!!["Unauthorized"]?.description)
    }

    @Test
    fun `components DSL should create components with parameters`() {
        val result = components {
            parameters {
                "PaginationLimit" to {
                    name = "limit"
                    location = ParameterLocation.QUERY
                    description = "Maximum number of items to return"
                    schema { type = "integer" }
                }
            }
        }

        assertNotNull(result.parameters)
        assertEquals(1, result.parameters!!.size)
        assertEquals("limit", result.parameters!!["PaginationLimit"]?.name)
    }

    @Test
    fun `components DSL should create components with examples`() {
        val result = components {
            examples {
                "PetExample" to {
                    summary = "A pet example"
                    value = JsonPrimitive("fluffy")
                }
            }
        }

        assertNotNull(result.examples)
        assertEquals(1, result.examples!!.size)
        assertEquals("A pet example", result.examples!!["PetExample"]?.summary)
    }

    @Test
    fun `components DSL should create components with request bodies`() {
        val result = components {
            requestBodies {
                "PetBody" to {
                    description = "Pet to add"
                    content {
                        "application/json" to {
                            schema { type = "object" }
                        }
                    }
                }
            }
        }

        assertNotNull(result.requestBodies)
        assertEquals(1, result.requestBodies!!.size)
        assertEquals("Pet to add", result.requestBodies!!["PetBody"]?.description)
    }

    @Test
    fun `components DSL should create components with headers`() {
        val result = components {
            headers {
                "X-Rate-Limit" to {
                    description = "Rate limit"
                    schema { type = "integer" }
                }
            }
        }

        assertNotNull(result.headers)
        assertEquals(1, result.headers!!.size)
        assertEquals("Rate limit", result.headers!!["X-Rate-Limit"]?.description)
    }

    @Test
    fun `components DSL should create components with security schemes`() {
        val result = components {
            securitySchemes {
                "api_key".apiKey {
                    name = "X-API-Key"
                    location = ApiKeyLocation.HEADER
                }
                "bearer_auth".http {
                    scheme = "bearer"
                    bearerFormat = "JWT"
                }
                "petstore_auth".oauth2 {
                    flows {
                        implicit {
                            authorizationUrl = "https://example.com/oauth/authorize"
                            scopes {
                                "read:pets" description "Read your pets"
                            }
                        }
                    }
                }
            }
        }

        assertNotNull(result.securitySchemes)
        assertEquals(3, result.securitySchemes!!.size)
        assertEquals(SecuritySchemeType.API_KEY, result.securitySchemes!!["api_key"]?.type)
        assertEquals(SecuritySchemeType.HTTP, result.securitySchemes!!["bearer_auth"]?.type)
        assertEquals(SecuritySchemeType.OAUTH2, result.securitySchemes!!["petstore_auth"]?.type)
    }

    @Test
    fun `components DSL should create components with links`() {
        val result = components {
            links {
                "GetUserById" to {
                    operationId = "getUser"
                    parameters = mapOf("userId" to "\$response.body#/id")
                }
            }
        }

        assertNotNull(result.links)
        assertEquals(1, result.links!!.size)
        assertEquals("getUser", result.links!!["GetUserById"]?.operationId)
    }

    @Test
    fun `components DSL should create components with callbacks`() {
        val result = components {
            callbacks {
                "onEvent" to {
                    "{${'$'}request.body#/callbackUrl}" to {
                        post {
                            responses {
                                ok { description = "Webhook received" }
                            }
                        }
                    }
                }
            }
        }

        assertNotNull(result.callbacks)
        assertEquals(1, result.callbacks!!.size)
    }

    @Test
    fun `components DSL should create components with path items`() {
        val result = components {
            pathItems {
                "CommonPathItem" to {
                    get {
                        summary = "Common get"
                        responses { ok { description = "OK" } }
                    }
                }
            }
        }

        assertNotNull(result.pathItems)
        assertEquals(1, result.pathItems!!.size)
    }

    @Test
    fun `components DSL should create components with media types`() {
        val result = components {
            mediaTypes {
                "JsonResponse" to {
                    schema { type = "object" }
                }
            }
        }

        assertNotNull(result.mediaTypes)
        assertEquals(1, result.mediaTypes!!.size)
    }

    @Test
    fun `components DSL should reject invalid component names`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            components {
                schemas = mapOf("invalid name!" to schema { type = "string" })
            }
        }

        assertTrue(exception.message!!.contains("must match pattern"))
    }

    @Test
    fun `components DSL should create components with all sections`() {
        val result = components {
            schemas {
                "Pet" to { type = "object" }
            }
            responses {
                "NotFound" to { description = "Not found" }
            }
            securitySchemes {
                "api_key".apiKey {
                    name = "api_key"
                    location = ApiKeyLocation.HEADER
                }
            }
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertNotNull(result.schemas)
        assertNotNull(result.responses)
        assertNotNull(result.securitySchemes)
        assertNotNull(result.extensions)
    }

    @Test
    fun `components DSL should serialize correctly`() {
        val result = components {
            schemas {
                "Error" to {
                    type = "object"
                    properties {
                        "message" to stringSchema()
                    }
                }
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"schemas\""))
        assertTrue(json.contains("\"Error\""))
    }

    @Test
    fun `components DSL should round-trip correctly`() {
        val result = components {
            schemas {
                "Pet" to { type = "object" }
            }
            securitySchemes {
                "api_key".apiKey {
                    name = "X-API-Key"
                    location = ApiKeyLocation.HEADER
                }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Components.serializer(), result)
    }

    // ============================================
    // OpenAPI Root DSL Tests
    // ============================================

    @Test
    fun `openAPI DSL should create minimal document with paths`() {
        val result = openAPI {
            info {
                title = "My API"
                version = "1.0.0"
            }
            paths {
                "/health" to {
                    get {
                        summary = "Health check"
                        responses { ok { description = "OK" } }
                    }
                }
            }
        }

        assertEquals("3.2.0", result.openapi)
        assertEquals("My API", result.info.title)
        assertEquals("1.0.0", result.info.version)
        assertNotNull(result.paths)
    }

    @Test
    fun `openAPI DSL should create minimal document with components only`() {
        val result = openAPI {
            info {
                title = "My API"
                version = "1.0.0"
            }
            components {
                schemas {
                    "Pet" to { type = "object" }
                }
            }
        }

        assertNotNull(result.components)
    }

    @Test
    fun `openAPI DSL should create minimal document with webhooks only`() {
        val result = openAPI {
            info {
                title = "My API"
                version = "1.0.0"
            }
            webhooks {
                "newPet" to {
                    post {
                        summary = "New pet notification"
                        responses { ok { description = "Acknowledged" } }
                    }
                }
            }
        }

        assertNotNull(result.webhooks)
        assertEquals(1, result.webhooks!!.size)
    }

    @Test
    fun `openAPI DSL should create document with all fields`() {
        val result = openAPI {
            openapi = "3.2.0"
            info {
                title = "Petstore API"
                version = "1.0.0"
                summary = "A sample Petstore API"
                description = "A sample API that uses a petstore"
                termsOfService = "https://example.com/terms/"
                contact {
                    name = "API Support"
                    email = "support@example.com"
                }
                license {
                    name = "Apache 2.0"
                    identifier = "Apache-2.0"
                }
            }
            jsonSchemaDialect = OpenAPI.DEFAULT_JSON_SCHEMA_DIALECT
            servers {
                server {
                    url = "https://api.example.com/v1"
                    description = "Production"
                }
                server {
                    url = "https://staging.example.com/v1"
                    description = "Staging"
                }
            }
            paths {
                "/pets" to {
                    get {
                        summary = "List pets"
                        operationId = "listPets"
                        tags("pets")
                        responses {
                            ok { description = "A list of pets" }
                        }
                    }
                    post {
                        summary = "Create pet"
                        operationId = "createPet"
                        tags("pets")
                        requestBody {
                            content {
                                "application/json" to {
                                    schema { type = "object" }
                                }
                            }
                        }
                        responses {
                            created { description = "Pet created" }
                        }
                    }
                }
            }
            components {
                schemas {
                    "Pet" to {
                        type = "object"
                        required("name")
                        properties {
                            "name" to stringSchema()
                            "tag" to stringSchema()
                        }
                    }
                }
                securitySchemes {
                    "api_key".apiKey {
                        name = "X-API-Key"
                        location = ApiKeyLocation.HEADER
                    }
                }
            }
            security {
                requirement("api_key")
            }
            tags {
                tag {
                    name = "pets"
                    description = "Pet operations"
                }
            }
            externalDocs {
                description = "Full documentation"
                url = "https://docs.example.com"
            }
            self = "https://api.example.com/openapi.json"
            extensions = mapOf("x-api-version" to JsonPrimitive("2025-01"))
        }

        assertEquals("3.2.0", result.openapi)
        assertEquals("Petstore API", result.info.title)
        assertEquals("1.0.0", result.info.version)
        assertEquals("A sample Petstore API", result.info.summary)
        assertNotNull(result.info.contact)
        assertNotNull(result.info.license)
        assertEquals(OpenAPI.DEFAULT_JSON_SCHEMA_DIALECT, result.jsonSchemaDialect)
        assertEquals(2, result.servers!!.size)
        assertNotNull(result.paths)
        assertNotNull(result.components)
        assertNotNull(result.security)
        assertEquals(1, result.tags!!.size)
        assertNotNull(result.externalDocs)
        assertEquals("https://api.example.com/openapi.json", result.self)
        assertNotNull(result.extensions)
    }

    @Test
    fun `openAPI DSL should fail when info is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            openAPI {
                paths {
                    "/test" to {
                        get {
                            responses { ok { description = "OK" } }
                        }
                    }
                }
            }
        }

        assertEquals("OpenAPI info is required", exception.message)
    }

    @Test
    fun `openAPI DSL should fail when none of paths, components, or webhooks is defined`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            openAPI {
                info {
                    title = "My API"
                    version = "1.0.0"
                }
            }
        }

        assertEquals(
            "OpenAPI document must have at least one of 'paths', 'components', or 'webhooks' defined",
            exception.message
        )
    }

    @Test
    fun `openAPI DSL should fail with invalid version`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            openAPI {
                openapi = "2.0"
                info {
                    title = "My API"
                    version = "1.0.0"
                }
                paths {
                    "/test" to {
                        get { responses { ok { description = "OK" } } }
                    }
                }
            }
        }

        assertTrue(exception.message!!.contains("must match pattern"))
    }

    @Test
    fun `openAPI DSL should fail when self contains fragment`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            openAPI {
                info {
                    title = "My API"
                    version = "1.0.0"
                }
                paths {
                    "/test" to {
                        get { responses { ok { description = "OK" } } }
                    }
                }
                self = "https://api.example.com/openapi.json#fragment"
            }
        }

        assertTrue(exception.message!!.contains("must not contain a fragment"))
    }

    @Test
    fun `openAPI DSL should default version to 3_2_0`() {
        val result = openAPI {
            info {
                title = "My API"
                version = "1.0.0"
            }
            components {
                schemas { "Test" to { type = "string" } }
            }
        }

        assertEquals("3.2.0", result.openapi)
    }

    @Test
    fun `openAPI DSL should serialize correctly`() {
        val result = openAPI {
            info {
                title = "Test API"
                version = "1.0.0"
            }
            paths {
                "/test" to {
                    get {
                        responses { ok { description = "OK" } }
                    }
                }
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"openapi\":\"3.2.0\""))
        assertTrue(json.contains("\"title\":\"Test API\""))
        assertTrue(json.contains("\"version\":\"1.0.0\""))
        assertTrue(json.contains("\"/test\""))
    }

    @Test
    fun `openAPI DSL should round-trip correctly`() {
        val result = openAPI {
            info {
                title = "Petstore"
                version = "1.0.0"
                contact {
                    name = "Support"
                }
            }
            servers {
                server {
                    url = "https://api.example.com"
                }
            }
            paths {
                "/pets" to {
                    get {
                        summary = "List pets"
                        responses { ok { description = "A list of pets" } }
                    }
                }
            }
            tags {
                tag { name = "pets" }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), result)
    }

    @Test
    fun `openAPI DSL should validate against OpenAPI schema`() {
        val result = openAPI {
            info {
                title = "Petstore"
                version = "1.0.0"
            }
            paths {
                "/pets" to {
                    get {
                        summary = "List all pets"
                        operationId = "listPets"
                        responses {
                            ok {
                                description = "A list of pets"
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "array"
                                            items = objectSchema {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        TestHelpers.testSerialization(OpenAPI.serializer(), result)
    }

    // ============================================
    // Webhooks DSL Tests
    // ============================================

    @Test
    fun `webhooks DSL should create empty webhooks`() {
        val result = webhooks {}

        assertEquals(emptyMap<String, PathItem>(), result)
    }

    @Test
    fun `webhooks DSL should create webhooks with path items`() {
        val result = webhooks {
            "newPet" to {
                post {
                    summary = "New pet was added"
                    requestBody {
                        content {
                            "application/json" to {
                                schema { type = "object" }
                            }
                        }
                    }
                    responses {
                        ok { description = "Webhook processed" }
                    }
                }
            }
            "petUpdated" to {
                post {
                    summary = "Pet was updated"
                    responses {
                        ok { description = "Acknowledged" }
                    }
                }
            }
        }

        assertEquals(2, result.size)
        assertTrue(result.containsKey("newPet"))
        assertTrue(result.containsKey("petUpdated"))
    }

    // ============================================
    // Integration Test: Full Petstore-like Document
    // ============================================

    @Test
    fun `openAPI DSL should build realistic Petstore API document`() {
        val petstoreApi = openAPI {
            info {
                title = "Petstore"
                version = "1.0.0"
                description = "A sample API that uses a petstore as an example to demonstrate features in the OpenAPI 3.2 specification"
                contact {
                    name = "Swagger API Team"
                    email = "apiteam@swagger.io"
                    url = "https://swagger.io"
                }
                license {
                    name = "Apache 2.0"
                    identifier = "Apache-2.0"
                }
            }
            servers {
                server {
                    url = "https://petstore.swagger.io/v2"
                    description = "Production server"
                }
            }
            paths {
                "/pets" to {
                    get {
                        summary = "List all pets"
                        operationId = "listPets"
                        tags("pets")
                        parameters {
                            queryParameter("limit", "How many items to return", false) {
                                type = "integer"
                                format = "int32"
                                maximum = 100
                            }
                        }
                        responses {
                            ok {
                                description = "A paged array of pets"
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "array"
                                            items = refSchema("#/components/schemas/Pet")
                                        }
                                    }
                                }
                            }
                            default {
                                description = "unexpected error"
                                content {
                                    "application/json" to {
                                        schema {
                                            ref = "#/components/schemas/Error"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    post {
                        summary = "Create a pet"
                        operationId = "createPet"
                        tags("pets")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema {
                                        ref = "#/components/schemas/Pet"
                                    }
                                }
                            }
                        }
                        responses {
                            created { description = "Null response" }
                            default {
                                description = "unexpected error"
                                content {
                                    "application/json" to {
                                        schema {
                                            ref = "#/components/schemas/Error"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "/pets/{petId}" to {
                    get {
                        summary = "Info for a specific pet"
                        operationId = "showPetById"
                        tags("pets")
                        parameters {
                            pathParameter("petId", "The id of the pet to retrieve") {
                                type = "string"
                            }
                        }
                        responses {
                            ok {
                                description = "Expected response to a valid request"
                                content {
                                    "application/json" to {
                                        schema {
                                            ref = "#/components/schemas/Pet"
                                        }
                                    }
                                }
                            }
                            default {
                                description = "unexpected error"
                                content {
                                    "application/json" to {
                                        schema {
                                            ref = "#/components/schemas/Error"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            components {
                schemas {
                    "Pet" to {
                        type = "object"
                        required("id", "name")
                        properties {
                            "id" to integerSchema { format = "int64" }
                            "name" to stringSchema()
                            "tag" to stringSchema()
                        }
                    }
                    "Error" to {
                        type = "object"
                        required("code", "message")
                        properties {
                            "code" to integerSchema { format = "int32" }
                            "message" to stringSchema()
                        }
                    }
                }
                securitySchemes {
                    "api_key".apiKey {
                        name = "api_key"
                        location = ApiKeyLocation.HEADER
                    }
                    "petstore_auth".oauth2 {
                        flows {
                            implicit {
                                authorizationUrl = "https://petstore.swagger.io/oauth/authorize"
                                scopes {
                                    "write:pets" description "modify pets in your account"
                                    "read:pets" description "read your pets"
                                }
                            }
                        }
                    }
                }
            }
            security {
                requirement("petstore_auth", "write:pets", "read:pets")
            }
            tags {
                tag {
                    name = "pets"
                    description = "Everything about your Pets"
                    externalDocs {
                        description = "Find out more"
                        url = "https://swagger.io"
                    }
                }
            }
        }

        // Verify structure
        assertEquals("3.2.0", petstoreApi.openapi)
        assertEquals("Petstore", petstoreApi.info.title)
        assertEquals(1, petstoreApi.servers!!.size)
        assertEquals(2, petstoreApi.paths!!.size)
        assertEquals(2, petstoreApi.components!!.schemas!!.size)
        assertEquals(2, petstoreApi.components!!.securitySchemes!!.size)
        assertEquals(1, petstoreApi.security!!.size)
        assertEquals(1, petstoreApi.tags!!.size)

        // Validate against OpenAPI schema
        TestHelpers.testSerialization(OpenAPI.serializer(), petstoreApi)

        // Verify round-trip
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), petstoreApi)
    }

    // ============================================
    // SecuritySchemes in Components - Additional Type DSL Tests
    // ============================================

    @Test
    fun `components securitySchemes should support mutualTLS via DSL`() {
        val result = components {
            securitySchemes {
                "mtls".mutualTLS {
                    description = "Mutual TLS"
                }
            }
        }

        assertEquals(SecuritySchemeType.MUTUAL_TLS, result.securitySchemes!!["mtls"]?.type)
    }

    @Test
    fun `components securitySchemes should support openIdConnect via DSL`() {
        val result = components {
            securitySchemes {
                "oidc".openIdConnect {
                    openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
                }
            }
        }

        assertEquals(SecuritySchemeType.OPEN_ID_CONNECT, result.securitySchemes!!["oidc"]?.type)
    }

    @Test
    fun `components securitySchemes should support pre-built schemes`() {
        val preBuilt = SecurityScheme.http(scheme = "basic")

        val result = components {
            securitySchemes {
                "basic_auth" to preBuilt
            }
        }

        assertEquals(SecuritySchemeType.HTTP, result.securitySchemes!!["basic_auth"]?.type)
        assertEquals("basic", result.securitySchemes!!["basic_auth"]?.scheme)
    }
}
