package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

/**
 * Integration tests that exercise the complete DSL surface area
 * and validate generated documents against the OpenAPI 3.2 JSON Schema.
 */
class DslIntegrationTest {

    @Test
    fun `should build comprehensive API document using only DSL`() {
        val api = openAPI {
            info {
                title = "E-Commerce Platform API"
                version = "3.0.0"
                summary = "Complete e-commerce platform API"
                description = "Manages products, orders, users, and payments"
                termsOfService = "https://shop.example.com/terms"
                contact {
                    name = "Platform Engineering"
                    url = "https://shop.example.com/developers"
                    email = "api@shop.example.com"
                }
                license {
                    name = "Apache 2.0"
                    identifier = "Apache-2.0"
                }
            }
            servers {
                server {
                    url = "https://{region}.api.shop.example.com/v3"
                    description = "Regional API server"
                    variables {
                        "region" to {
                            default = "us"
                            enum("us", "eu", "ap")
                            description = "API region"
                        }
                    }
                }
                server {
                    url = "https://sandbox.api.shop.example.com/v3"
                    description = "Sandbox environment"
                }
            }
            paths {
                "/products" to {
                    summary = "Product operations"
                    get {
                        summary = "List products"
                        operationId = "listProducts"
                        tags("products")
                        parameters {
                            queryParameter("limit", "Number of items to return", false) {
                                type = "integer"
                                format = "int32"
                                minimum = 1
                                maximum = 100
                            }
                            queryParameter("offset", "Offset for pagination", false) {
                                type = "integer"
                                format = "int32"
                                minimum = 0
                            }
                            queryParameter("category", "Filter by category") {
                                type = "string"
                            }
                            headerParameter("X-Correlation-Id", "Request correlation ID") {
                                type = "string"
                                format = "uuid"
                            }
                        }
                        security {
                            requirement("api_key")
                            requirement("oauth2", "products:read")
                        }
                        responses {
                            ok {
                                description = "A paginated list of products"
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "object"
                                            properties {
                                                "data" to arraySchema {
                                                    items = refSchema("#/components/schemas/Product")
                                                }
                                                "total" to integerSchema()
                                                "limit" to integerSchema()
                                                "offset" to integerSchema()
                                            }
                                        }
                                    }
                                }
                            }
                            badRequest {
                                description = "Invalid query parameters"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Error" }
                                    }
                                }
                            }
                            unauthorized {
                                description = "Missing or invalid credentials"
                            }
                        }
                    }
                    post {
                        summary = "Create a product"
                        operationId = "createProduct"
                        tags("products")
                        security {
                            requirement("oauth2", "products:write")
                        }
                        requestBody {
                            description = "Product to create"
                            required = true
                            content {
                                "application/json" to {
                                    schema { ref = "#/components/schemas/CreateProductRequest" }
                                }
                            }
                        }
                        responses {
                            created {
                                description = "Product created"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Product" }
                                    }
                                }
                            }
                            badRequest {
                                description = "Validation error"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Error" }
                                    }
                                }
                            }
                        }
                    }
                }
                "/products/{productId}" to {
                    parameters {
                        pathParameter("productId", "The product ID") {
                            type = "string"
                            format = "uuid"
                        }
                    }
                    get {
                        summary = "Get product by ID"
                        operationId = "getProduct"
                        tags("products")
                        responses {
                            ok {
                                description = "Product details"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Product" }
                                    }
                                }
                            }
                            notFound {
                                description = "Product not found"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Error" }
                                    }
                                }
                            }
                        }
                    }
                    put {
                        summary = "Update product"
                        operationId = "updateProduct"
                        tags("products")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema { ref = "#/components/schemas/CreateProductRequest" }
                                }
                            }
                        }
                        responses {
                            ok {
                                description = "Product updated"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Product" }
                                    }
                                }
                            }
                            notFound { description = "Product not found" }
                        }
                    }
                    delete {
                        summary = "Delete product"
                        operationId = "deleteProduct"
                        tags("products")
                        deprecated = true
                        responses {
                            noContent { description = "Product deleted" }
                            notFound { description = "Product not found" }
                        }
                    }
                }
                "/orders" to {
                    post {
                        summary = "Place an order"
                        operationId = "placeOrder"
                        tags("orders")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema { ref = "#/components/schemas/CreateOrderRequest" }
                                }
                            }
                        }
                        callbacks {
                            "orderStatusUpdate" to {
                                "{${'$'}request.body#/callbackUrl}" to {
                                    post {
                                        summary = "Order status webhook"
                                        requestBody {
                                            required = true
                                            content {
                                                "application/json" to {
                                                    schema {
                                                        type = "object"
                                                        properties {
                                                            "orderId" to stringSchema { format = "uuid" }
                                                            "status" to stringSchema()
                                                            "timestamp" to stringSchema { format = "date-time" }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        responses {
                                            ok { description = "Callback received" }
                                        }
                                    }
                                }
                            }
                        }
                        responses {
                            created {
                                description = "Order placed"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Order" }
                                    }
                                }
                            }
                            badRequest { description = "Invalid order" }
                        }
                    }
                }
            }
            webhooks {
                "inventoryUpdate" to {
                    post {
                        summary = "Inventory level changed"
                        operationId = "onInventoryUpdate"
                        tags("webhooks")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema {
                                        type = "object"
                                        properties {
                                            "productId" to stringSchema { format = "uuid" }
                                            "quantity" to integerSchema()
                                            "warehouse" to stringSchema()
                                        }
                                    }
                                }
                            }
                        }
                        responses {
                            ok { description = "Webhook acknowledged" }
                        }
                    }
                }
            }
            components {
                schemas {
                    "Product" to {
                        type = "object"
                        required("id", "name", "price")
                        properties {
                            "id" to stringSchema { format = "uuid" }
                            "name" to stringSchema { minLength = 1; maxLength = 255 }
                            "description" to stringSchema()
                            "price" to numberSchema { minimum = 0; format = "double" }
                            "category" to stringSchema()
                            "tags" to arraySchema { items = stringSchema(); uniqueItems = true }
                            "createdAt" to stringSchema { format = "date-time" }
                        }
                    }
                    "CreateProductRequest" to {
                        type = "object"
                        required("name", "price")
                        properties {
                            "name" to stringSchema { minLength = 1 }
                            "description" to stringSchema()
                            "price" to numberSchema { minimum = 0 }
                            "category" to stringSchema()
                            "tags" to arraySchema { items = stringSchema() }
                        }
                    }
                    "Order" to {
                        type = "object"
                        required("id", "items", "status")
                        properties {
                            "id" to stringSchema { format = "uuid" }
                            "items" to arraySchema {
                                items = objectSchema {
                                    properties {
                                        "productId" to stringSchema { format = "uuid" }
                                        "quantity" to integerSchema { minimum = 1 }
                                    }
                                }
                                minItems = 1
                            }
                            "status" to stringSchema()
                            "total" to numberSchema { format = "double" }
                            "callbackUrl" to stringSchema { format = "uri" }
                            "createdAt" to stringSchema { format = "date-time" }
                        }
                    }
                    "CreateOrderRequest" to {
                        type = "object"
                        required("items")
                        properties {
                            "items" to arraySchema {
                                items = objectSchema {
                                    properties {
                                        "productId" to stringSchema { format = "uuid" }
                                        "quantity" to integerSchema { minimum = 1 }
                                    }
                                }
                                minItems = 1
                            }
                            "callbackUrl" to stringSchema { format = "uri" }
                        }
                    }
                    "Error" to {
                        type = "object"
                        required("code", "message")
                        properties {
                            "code" to integerSchema { format = "int32" }
                            "message" to stringSchema()
                            "details" to arraySchema { items = stringSchema() }
                        }
                    }
                }
                responses {
                    "NotFound" to {
                        description = "Resource not found"
                        content {
                            "application/json" to {
                                schema { ref = "#/components/schemas/Error" }
                            }
                        }
                    }
                    "Unauthorized" to {
                        description = "Authentication required"
                    }
                }
                parameters {
                    "PaginationLimit" to {
                        name = "limit"
                        location = ParameterLocation.QUERY
                        description = "Maximum number of items to return"
                        schema {
                            type = "integer"
                            format = "int32"
                            minimum = 1
                            maximum = 100
                        }
                    }
                    "PaginationOffset" to {
                        name = "offset"
                        location = ParameterLocation.QUERY
                        description = "Number of items to skip"
                        schema {
                            type = "integer"
                            format = "int32"
                            minimum = 0
                        }
                    }
                }
                headers {
                    "X-Rate-Limit-Remaining" to {
                        description = "Number of requests remaining"
                        schema { type = "integer" }
                    }
                    "X-Request-Id" to {
                        description = "Unique request identifier"
                        schema { type = "string"; format = "uuid" }
                    }
                }
                securitySchemes {
                    "api_key".apiKey {
                        name = "X-API-Key"
                        location = ApiKeyLocation.HEADER
                        description = "API key authentication"
                    }
                    "oauth2".oauth2 {
                        description = "OAuth2 authentication"
                        flows {
                            authorizationCode {
                                authorizationUrl = "https://auth.shop.example.com/authorize"
                                tokenUrl = "https://auth.shop.example.com/token"
                                refreshUrl = "https://auth.shop.example.com/refresh"
                                scopes {
                                    "products:read" description "Read product catalog"
                                    "products:write" description "Manage products"
                                    "orders:read" description "View orders"
                                    "orders:write" description "Place and manage orders"
                                }
                            }
                            clientCredentials {
                                tokenUrl = "https://auth.shop.example.com/token"
                                scopes {
                                    "products:read" description "Read product catalog"
                                }
                            }
                        }
                    }
                    "bearer".http {
                        scheme = "bearer"
                        bearerFormat = "JWT"
                        description = "JWT Bearer token"
                    }
                }
                links {
                    "GetProductById" to {
                        operationId = "getProduct"
                        parameters = mapOf("productId" to "\$response.body#/id")
                        description = "Get the created/updated product"
                    }
                }
            }
            security {
                requirement("api_key")
            }
            tags {
                tag {
                    name = "products"
                    description = "Product catalog management"
                }
                tag {
                    name = "orders"
                    description = "Order management"
                }
                tag {
                    name = "webhooks"
                    description = "Webhook events"
                }
            }
            externalDocs {
                description = "Full API documentation"
                url = "https://docs.shop.example.com"
            }
        }

        // Validate structure
        assertEquals("3.2.0", api.openapi)
        assertEquals("E-Commerce Platform API", api.info.title)
        assertEquals("3.0.0", api.info.version)
        assertNotNull(api.info.contact)
        assertNotNull(api.info.license)
        assertEquals(2, api.servers!!.size)
        assertEquals(3, api.paths!!.size)
        assertEquals(1, api.webhooks!!.size)
        assertEquals(5, api.components!!.schemas!!.size)
        assertEquals(2, api.components.responses!!.size)
        assertEquals(2, api.components.parameters!!.size)
        assertEquals(2, api.components.headers!!.size)
        assertEquals(3, api.components.securitySchemes!!.size)
        assertEquals(1, api.components.links!!.size)
        assertEquals(1, api.security!!.size)
        assertEquals(3, api.tags!!.size)
        assertNotNull(api.externalDocs)

        // Validate against OpenAPI 3.2 JSON Schema
        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)

        // Verify round-trip
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build API with all security scheme types`() {
        val api = openAPI {
            info {
                title = "Multi-Auth API"
                version = "1.0.0"
            }
            components {
                securitySchemes {
                    "api_key_header".apiKey {
                        name = "X-API-Key"
                        location = ApiKeyLocation.HEADER
                        description = "API key in header"
                    }
                    "api_key_query".apiKey {
                        name = "api_key"
                        location = ApiKeyLocation.QUERY
                    }
                    "api_key_cookie".apiKey {
                        name = "session"
                        location = ApiKeyLocation.COOKIE
                    }
                    "basic_auth".http {
                        scheme = "basic"
                        description = "Basic HTTP authentication"
                    }
                    "bearer_auth".http {
                        scheme = "bearer"
                        bearerFormat = "JWT"
                    }
                    "mutual_tls".mutualTLS {
                        description = "Mutual TLS client certificate"
                    }
                    "oauth2_full".oauth2 {
                        description = "Full OAuth2 with all flows"
                        flows {
                            implicit {
                                authorizationUrl = "https://auth.example.com/authorize"
                                scopes { "read" description "Read" }
                            }
                            password {
                                tokenUrl = "https://auth.example.com/token"
                                scopes { "read" description "Read" }
                            }
                            clientCredentials {
                                tokenUrl = "https://auth.example.com/token"
                                scopes { "admin" description "Admin" }
                            }
                            authorizationCode {
                                authorizationUrl = "https://auth.example.com/authorize"
                                tokenUrl = "https://auth.example.com/token"
                                refreshUrl = "https://auth.example.com/refresh"
                                scopes {
                                    "read" description "Read access"
                                    "write" description "Write access"
                                }
                            }
                            deviceAuthorization {
                                deviceAuthorizationUrl = "https://auth.example.com/device"
                                tokenUrl = "https://auth.example.com/token"
                                scopes { "read" description "Read" }
                            }
                        }
                    }
                    "oidc".openIdConnect {
                        openIdConnectUrl = "https://auth.example.com/.well-known/openid-configuration"
                        description = "OpenID Connect Discovery"
                    }
                }
            }
            security {
                requirement("bearer_auth")
                requirement("api_key_header")
                requirements(
                    "oauth2_full" to listOf("read"),
                    "api_key_header" to emptyList<String>()
                )
            }
        }

        assertEquals(8, api.components!!.securitySchemes!!.size)
        assertEquals(3, api.security!!.size)

        // Validate against OpenAPI 3.2 JSON Schema
        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build API with all HTTP methods`() {
        val api = openAPI {
            info {
                title = "All Methods API"
                version = "1.0.0"
            }
            paths {
                "/resource" to {
                    get {
                        operationId = "getResource"
                        responses { ok { description = "OK" } }
                    }
                    post {
                        operationId = "createResource"
                        responses { created { description = "Created" } }
                    }
                    put {
                        operationId = "replaceResource"
                        responses { ok { description = "Replaced" } }
                    }
                    patch {
                        operationId = "updateResource"
                        responses { ok { description = "Updated" } }
                    }
                    delete {
                        operationId = "deleteResource"
                        responses { noContent { description = "Deleted" } }
                    }
                    head {
                        operationId = "headResource"
                        responses { ok { description = "OK" } }
                    }
                    options {
                        operationId = "optionsResource"
                        responses { ok { description = "OK" } }
                    }
                    trace {
                        operationId = "traceResource"
                        responses { ok { description = "OK" } }
                    }
                }
            }
        }

        val path = api.paths!!["/resource"]!!
        assertNotNull(path.get)
        assertNotNull(path.post)
        assertNotNull(path.put)
        assertNotNull(path.patch)
        assertNotNull(path.delete)
        assertNotNull(path.head)
        assertNotNull(path.options)
        assertNotNull(path.trace)

        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build API with parameter styles and content negotiation`() {
        val api = openAPI {
            info {
                title = "Rich Parameters API"
                version = "1.0.0"
            }
            paths {
                "/search" to {
                    get {
                        operationId = "search"
                        parameters {
                            queryParameter("q", "Search query", true) {
                                type = "string"
                                minLength = 1
                            }
                            queryParameter("tags", "Filter tags") {
                                type = "array"
                                items = stringSchema()
                            }
                            headerParameter("Accept-Language", "Preferred language") {
                                type = "string"
                            }
                            cookieParameter("preferences", "User preferences") {
                                type = "string"
                            }
                        }
                        responses {
                            ok {
                                description = "Search results"
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "object"
                                            properties {
                                                "results" to arraySchema {
                                                    items = objectSchema {}
                                                }
                                                "count" to integerSchema()
                                            }
                                        }
                                    }
                                    "application/xml" to {
                                        schema { type = "object" }
                                    }
                                }
                            }
                            "5XX" to {
                                description = "Server error"
                            }
                        }
                    }
                }
            }
        }

        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build webhooks-only API document`() {
        val api = openAPI {
            info {
                title = "Event Notification Service"
                version = "1.0.0"
                description = "Webhooks-only API for event notifications"
            }
            webhooks {
                "orderCreated" to {
                    post {
                        operationId = "onOrderCreated"
                        tags("orders")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema {
                                        type = "object"
                                        required("event", "data")
                                        properties {
                                            "event" to stringSchema()
                                            "data" to objectSchema {
                                                properties {
                                                    "orderId" to stringSchema()
                                                    "amount" to numberSchema()
                                                }
                                            }
                                            "timestamp" to stringSchema { format = "date-time" }
                                        }
                                    }
                                }
                            }
                        }
                        responses {
                            ok { description = "Event processed" }
                        }
                    }
                }
                "paymentReceived" to {
                    post {
                        operationId = "onPaymentReceived"
                        tags("payments")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema {
                                        type = "object"
                                        properties {
                                            "paymentId" to stringSchema()
                                            "orderId" to stringSchema()
                                            "amount" to numberSchema()
                                            "currency" to stringSchema()
                                        }
                                    }
                                }
                            }
                        }
                        responses {
                            ok { description = "Payment acknowledged" }
                        }
                    }
                }
            }
        }

        assertEquals(2, api.webhooks!!.size)
        assertTrue(api.webhooks.containsKey("orderCreated"))
        assertTrue(api.webhooks.containsKey("paymentReceived"))

        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build API with server variables and multiple servers`() {
        val api = openAPI {
            info {
                title = "Multi-Region API"
                version = "2.0.0"
            }
            servers {
                server {
                    url = "https://{region}.api.example.com/{version}"
                    description = "Regional production server"
                    variables {
                        "region" to {
                            default = "us-east"
                            enum("us-east", "us-west", "eu-central", "ap-southeast")
                            description = "Deployment region"
                        }
                        "version" to {
                            default = "v2"
                            enum("v1", "v2")
                            description = "API version"
                        }
                    }
                }
                server {
                    url = "https://localhost:{port}"
                    description = "Local development"
                    variables {
                        "port" to {
                            default = "8080"
                            description = "Server port"
                        }
                    }
                }
            }
            paths {
                "/health" to {
                    get {
                        operationId = "healthCheck"
                        responses { ok { description = "Healthy" } }
                    }
                }
            }
        }

        assertEquals(2, api.servers!!.size)
        assertEquals(2, api.servers[0].variables!!.size)
        assertEquals(1, api.servers[1].variables!!.size)

        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build API with extensions throughout`() {
        val api = openAPI {
            info {
                title = "Extended API"
                version = "1.0.0"
                extensions = mapOf("x-api-id" to JsonPrimitive("ext-api-001"))
            }
            paths {
                "/data" to {
                    get {
                        operationId = "getData"
                        extensions = mapOf("x-rate-limit" to JsonPrimitive(100))
                        responses {
                            ok { description = "Data returned" }
                        }
                    }
                }
            }
            components {
                schemas {
                    "Data" to { type = "object" }
                }
            }
            extensions = mapOf(
                "x-generated-by" to JsonPrimitive("komok"),
                "x-spec-version" to JsonPrimitive("3.2.0")
            )
        }

        assertNotNull(api.extensions)
        assertEquals(2, api.extensions.size)
        assertNotNull(api.info.extensions)

        // Extensions are serialized as nested "extensions" field, not flattened as x- properties,
        // so we skip OpenAPI schema validation here and only verify round-trip correctness
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build GitHub-style API document`() {
        val api = openAPI {
            info {
                title = "GitHub-style Repository API"
                version = "2022-11-28"
                description = "A subset of GitHub-like API demonstrating real-world patterns"
                contact {
                    url = "https://developer.example.com"
                }
            }
            servers {
                server {
                    url = "https://api.example.com"
                }
            }
            paths {
                "/repos/{owner}/{repo}" to {
                    parameters {
                        pathParameter("owner", "The account owner") { type = "string" }
                        pathParameter("repo", "The repository name") { type = "string" }
                    }
                    get {
                        summary = "Get a repository"
                        operationId = "repos.get"
                        tags("repos")
                        externalDocs {
                            url = "https://docs.example.com/rest/repos/repos#get-a-repository"
                        }
                        responses {
                            ok {
                                description = "Repository details"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Repository" }
                                    }
                                }
                            }
                            notFound { description = "Repository not found" }
                            "403" to { description = "Forbidden" }
                        }
                    }
                }
                "/repos/{owner}/{repo}/issues" to {
                    parameters {
                        pathParameter("owner") { type = "string" }
                        pathParameter("repo") { type = "string" }
                    }
                    get {
                        summary = "List repository issues"
                        operationId = "issues.list"
                        tags("issues")
                        parameters {
                            queryParameter("state", "Filter by state") {
                                type = "string"
                                enum = listOf(
                                    JsonPrimitive("open"),
                                    JsonPrimitive("closed"),
                                    JsonPrimitive("all")
                                )
                            }
                            queryParameter("sort", "Sort field") {
                                type = "string"
                            }
                            queryParameter("per_page", "Results per page", false) {
                                type = "integer"
                                minimum = 1
                                maximum = 100
                            }
                            queryParameter("page", "Page number", false) {
                                type = "integer"
                                minimum = 1
                            }
                        }
                        responses {
                            ok {
                                description = "List of issues"
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "array"
                                            items = refSchema("#/components/schemas/Issue")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    post {
                        summary = "Create an issue"
                        operationId = "issues.create"
                        tags("issues")
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema {
                                        type = "object"
                                        required("title")
                                        properties {
                                            "title" to stringSchema { minLength = 1 }
                                            "body" to stringSchema()
                                            "labels" to arraySchema { items = stringSchema() }
                                            "assignees" to arraySchema { items = stringSchema() }
                                        }
                                    }
                                }
                            }
                        }
                        responses {
                            created {
                                description = "Issue created"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Issue" }
                                    }
                                }
                            }
                            "422" to { description = "Validation failed" }
                        }
                    }
                }
            }
            components {
                schemas {
                    "Repository" to {
                        type = "object"
                        required("id", "name", "full_name", "owner")
                        properties {
                            "id" to integerSchema { format = "int64" }
                            "name" to stringSchema()
                            "full_name" to stringSchema()
                            "owner" to objectSchema {
                                properties {
                                    "login" to stringSchema()
                                    "id" to integerSchema()
                                    "avatar_url" to stringSchema { format = "uri" }
                                }
                            }
                            "private" to genericSchema { type = "boolean" }
                            "description" to stringSchema()
                            "fork" to genericSchema { type = "boolean" }
                            "stargazers_count" to integerSchema()
                            "language" to stringSchema()
                        }
                    }
                    "Issue" to {
                        type = "object"
                        required("id", "number", "title", "state")
                        properties {
                            "id" to integerSchema { format = "int64" }
                            "number" to integerSchema()
                            "title" to stringSchema()
                            "state" to stringSchema()
                            "body" to stringSchema()
                            "labels" to arraySchema {
                                items = objectSchema {
                                    properties {
                                        "id" to integerSchema()
                                        "name" to stringSchema()
                                        "color" to stringSchema()
                                    }
                                }
                            }
                            "created_at" to stringSchema { format = "date-time" }
                            "updated_at" to stringSchema { format = "date-time" }
                        }
                    }
                }
                securitySchemes {
                    "token".http {
                        scheme = "bearer"
                        description = "Personal access token"
                    }
                }
            }
            security {
                requirement("token")
            }
            tags {
                tag {
                    name = "repos"
                    description = "Repository operations"
                }
                tag {
                    name = "issues"
                    description = "Issue tracking"
                }
            }
        }

        // Verify structure
        assertEquals(2, api.paths!!.size)
        assertEquals(2, api.components!!.schemas!!.size)

        // Validate against OpenAPI 3.2 JSON Schema
        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build Stripe-style API document`() {
        val api = openAPI {
            info {
                title = "Stripe-style Payments API"
                version = "2024-01-01"
                description = "A Stripe-inspired payments API"
            }
            servers {
                server { url = "https://api.payments.example.com/v1" }
            }
            paths {
                "/charges" to {
                    post {
                        summary = "Create a charge"
                        operationId = "createCharge"
                        tags("charges")
                        requestBody {
                            required = true
                            content {
                                "application/x-www-form-urlencoded" to {
                                    schema {
                                        type = "object"
                                        required("amount", "currency")
                                        properties {
                                            "amount" to integerSchema { minimum = 50; description = "Amount in cents" }
                                            "currency" to stringSchema { minLength = 3; maxLength = 3 }
                                            "customer" to stringSchema()
                                            "description" to stringSchema()
                                            "metadata" to objectSchema {
                                                additionalProperties = JsonPrimitive("string")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        responses {
                            ok {
                                description = "Charge created"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Charge" }
                                    }
                                }
                            }
                            badRequest { description = "Invalid parameters" }
                            unauthorized { description = "Invalid API key" }
                        }
                    }
                    get {
                        summary = "List charges"
                        operationId = "listCharges"
                        tags("charges")
                        parameters {
                            queryParameter("limit", "Number of charges to return", false) {
                                type = "integer"
                                minimum = 1
                                maximum = 100
                            }
                            queryParameter("starting_after", "Pagination cursor") {
                                type = "string"
                            }
                        }
                        responses {
                            ok {
                                description = "List of charges"
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "object"
                                            required("data", "has_more")
                                            properties {
                                                "data" to arraySchema {
                                                    items = refSchema("#/components/schemas/Charge")
                                                }
                                                "has_more" to genericSchema { type = "boolean" }
                                                "url" to stringSchema()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "/charges/{chargeId}" to {
                    parameters {
                        pathParameter("chargeId", "The charge ID") { type = "string" }
                    }
                    get {
                        summary = "Retrieve a charge"
                        operationId = "getCharge"
                        tags("charges")
                        responses {
                            ok {
                                description = "Charge details"
                                content {
                                    "application/json" to {
                                        schema { ref = "#/components/schemas/Charge" }
                                    }
                                }
                            }
                            notFound { description = "Charge not found" }
                        }
                    }
                }
            }
            components {
                schemas {
                    "Charge" to {
                        type = "object"
                        required("id", "amount", "currency", "status")
                        properties {
                            "id" to stringSchema()
                            "object" to stringSchema()
                            "amount" to integerSchema()
                            "currency" to stringSchema()
                            "status" to stringSchema()
                            "customer" to stringSchema()
                            "description" to stringSchema()
                            "created" to integerSchema { description = "Unix timestamp" }
                            "metadata" to objectSchema {
                                additionalProperties = JsonPrimitive("string")
                            }
                        }
                    }
                }
                securitySchemes {
                    "bearerAuth".http {
                        scheme = "bearer"
                        description = "Secret API key as bearer token"
                    }
                }
            }
            security {
                requirement("bearerAuth")
            }
            tags {
                tag {
                    name = "charges"
                    description = "Charge operations"
                }
            }
        }

        assertEquals(2, api.paths!!.size)
        assertEquals(1, api.components!!.schemas!!.size)

        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `should build API with deeply nested schemas`() {
        val api = openAPI {
            info {
                title = "Nested Schema API"
                version = "1.0.0"
            }
            components {
                schemas {
                    "DeepObject" to {
                        type = "object"
                        properties {
                            "level1" to objectSchema {
                                properties {
                                    "level2" to objectSchema {
                                        properties {
                                            "level3" to objectSchema {
                                                properties {
                                                    "value" to stringSchema()
                                                    "items" to arraySchema {
                                                        items = objectSchema {
                                                            properties {
                                                                "id" to integerSchema()
                                                                "tags" to arraySchema {
                                                                    items = stringSchema()
                                                                    uniqueItems = true
                                                                }
                                                            }
                                                        }
                                                        maxItems = 50
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "metadata" to objectSchema {
                                additionalProperties = JsonPrimitive(true)
                            }
                        }
                    }
                }
            }
        }

        val _ = TestHelpers.testSerialization(OpenAPI.serializer(), api)
        TestHelpers.testRoundTripWithoutValidation(OpenAPI.serializer(), api)
    }

    @Test
    fun `full document should validate against OpenAPI JSON schema`() {
        val api = openAPI {
            info {
                title = "Schema Validation Test API"
                version = "1.0.0"
            }
            paths {
                "/items" to {
                    get {
                        operationId = "listItems"
                        responses {
                            ok { description = "Success" }
                            default { description = "Error" }
                        }
                    }
                    post {
                        operationId = "createItem"
                        requestBody {
                            required = true
                            content {
                                "application/json" to {
                                    schema { type = "object" }
                                }
                            }
                        }
                        responses {
                            created { description = "Created" }
                            badRequest { description = "Invalid input" }
                            unauthorized { description = "Not authorized" }
                            forbidden { description = "Forbidden" }
                            internalServerError { description = "Server error" }
                        }
                    }
                }
            }
        }

        val json = openApiJson.encodeToString(api)

        // Write for manual inspection
        val outputFile = java.io.File("build/dsl-integration-test.json")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json)

        // Validate against schema
        val _ = OpenAPISchemaValidator.validate(json)
    }
}
