package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OperationAndPathDslTest {

    // ============================================
    // ReferenceableParameters DSL Tests
    // ============================================

    @Test
    fun `referenceableParameters DSL should create list with inline parameters`() {
        val result = referenceableParameters {
            parameter {
                name = "userId"
                location = ParameterLocation.PATH
                required = true
                schema { type = "string" }
            }
        }

        assertEquals(1, result.size)
        assertTrue(result[0] is Direct)
        assertEquals("userId", (result[0] as Direct<Parameter>).value.name)
    }

    @Test
    fun `referenceableParameters DSL should create list with references`() {
        val result = referenceableParameters {
            ref("#/components/parameters/PaginationLimit")
        }

        assertEquals(1, result.size)
        assertTrue(result[0] is Reference)
        assertEquals("#/components/parameters/PaginationLimit", (result[0] as Reference).ref)
    }

    @Test
    fun `referenceableParameters DSL should create mixed list`() {
        val result = referenceableParameters {
            parameter {
                name = "userId"
                location = ParameterLocation.PATH
                required = true
                schema { type = "string" }
            }
            ref("#/components/parameters/Limit")
            ref("#/components/parameters/Offset", summary = "Pagination offset")
        }

        assertEquals(3, result.size)
        assertTrue(result[0] is Direct)
        assertTrue(result[1] is Reference)
        assertTrue(result[2] is Reference)
        assertEquals("Pagination offset", (result[2] as Reference).summary)
    }

    @Test
    fun `referenceableParameters DSL should support convenience methods`() {
        val result = referenceableParameters {
            pathParameter("userId", "The user ID") { format = "uuid" }
            queryParameter("limit", "Max results", required = false) { type = "integer" }
            headerParameter("X-Api-Key", "API key", required = true) { type = "string" }
            cookieParameter("session", "Session ID") { type = "string" }
        }

        assertEquals(4, result.size)
        assertEquals("userId", (result[0] as Direct<Parameter>).value.name)
        assertEquals(ParameterLocation.PATH, (result[0] as Direct<Parameter>).value.location)
        assertEquals("limit", (result[1] as Direct<Parameter>).value.name)
        assertEquals(ParameterLocation.QUERY, (result[1] as Direct<Parameter>).value.location)
        assertEquals("X-Api-Key", (result[2] as Direct<Parameter>).value.name)
        assertEquals(ParameterLocation.HEADER, (result[2] as Direct<Parameter>).value.location)
        assertEquals("session", (result[3] as Direct<Parameter>).value.name)
        assertEquals(ParameterLocation.COOKIE, (result[3] as Direct<Parameter>).value.location)
    }

    @Test
    fun `referenceableParameters DSL should accept pre-built parameters`() {
        val preBuilt = Parameter(
            name = "prebuilt",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "string") })
        )

        val result = referenceableParameters {
            parameter(preBuilt)
        }

        assertEquals(1, result.size)
        assertEquals("prebuilt", (result[0] as Direct<Parameter>).value.name)
    }

    // ============================================
    // SecurityRequirements DSL Tests
    // ============================================

    @Test
    fun `securityRequirements DSL should create list with single requirement`() {
        val result = securityRequirements {
            requirement("api_key")
        }

        assertEquals(1, result.size)
        assertEquals(listOf<String>(), result[0]["api_key"])
    }

    @Test
    fun `securityRequirements DSL should create list with scopes`() {
        val result = securityRequirements {
            requirement("oauth2", "read:users", "write:users")
        }

        assertEquals(1, result.size)
        assertEquals(listOf("read:users", "write:users"), result[0]["oauth2"])
    }

    @Test
    fun `securityRequirements DSL should create list with multiple requirements`() {
        val result = securityRequirements {
            requirement("api_key")
            requirement("oauth2", "read")
        }

        assertEquals(2, result.size)
    }

    @Test
    fun `securityRequirements DSL should support AND logic`() {
        val result = securityRequirements {
            requirements(
                "api_key" to listOf(),
                "basic_auth" to listOf()
            )
        }

        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
    }

    // ============================================
    // Operation DSL Tests
    // ============================================

    @Test
    fun `operation DSL should create with minimal responses`() {
        val result = operation {
            responses {
                ok { description = "Success" }
            }
        }

        assertEquals(1, result.responses.size)
        assertEquals("Success", result.responses["200"]?.description)
    }

    @Test
    fun `operation DSL should create with all properties`() {
        val result = operation {
            summary = "Get user by ID"
            description = "Returns a single user"
            operationId = "getUserById"
            tags("users", "public")
            deprecated = true
            extensions = mapOf("x-internal" to JsonPrimitive(true))
            responses {
                ok { description = "Success" }
            }
        }

        assertEquals("Get user by ID", result.summary)
        assertEquals("Returns a single user", result.description)
        assertEquals("getUserById", result.operationId)
        assertEquals(listOf("users", "public"), result.tags)
        assertEquals(true, result.deprecated)
        assertEquals(mapOf("x-internal" to JsonPrimitive(true)), result.extensions)
    }

    @Test
    fun `operation DSL should support parameters`() {
        val result = operation {
            parameters {
                pathParameter("userId") { format = "uuid" }
                queryParameter("include") { type = "string" }
            }
            responses {
                ok { description = "Success" }
            }
        }

        assertEquals(2, result.parameters?.size)
    }

    @Test
    fun `operation DSL should support requestBody`() {
        val result = operation {
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
            }
        }

        assertNotNull(result.requestBody)
        assertEquals(true, result.requestBody?.required)
    }

    @Test
    fun `operation DSL should support externalDocs`() {
        val result = operation {
            externalDocs {
                url = "https://docs.example.com"
                description = "More info"
            }
            responses {
                ok { description = "Success" }
            }
        }

        assertNotNull(result.externalDocs)
        assertEquals("https://docs.example.com", result.externalDocs?.url)
    }

    @Test
    fun `operation DSL should support security`() {
        val result = operation {
            security {
                requirement("api_key")
                requirement("oauth2", "read:users")
            }
            responses {
                ok { description = "Success" }
            }
        }

        assertEquals(2, result.security?.size)
    }

    @Test
    fun `operation DSL should support servers`() {
        val result = operation {
            servers {
                server {
                    url = "https://api.example.com"
                }
            }
            responses {
                ok { description = "Success" }
            }
        }

        assertEquals(1, result.servers?.size)
    }

    @Test
    fun `operation DSL should fail when responses is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            operation {
                summary = "Missing responses"
            }
        }

        assertEquals("Operation responses is required", exception.message)
    }

    @Test
    fun `operation DSL should serialize correctly`() {
        val result = operation {
            summary = "Get user"
            operationId = "getUser"
            responses {
                ok { description = "Success" }
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"summary\":\"Get user\""))
        assertTrue(json.contains("\"operationId\":\"getUser\""))
        assertTrue(json.contains("\"200\""))
    }

    @Test
    fun `operation DSL should round-trip correctly`() {
        val result = operation {
            summary = "Test operation"
            operationId = "testOp"
            tags("test")
            responses {
                ok { description = "OK" }
                badRequest { description = "Bad Request" }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Operation.serializer(), result)
    }

    // ============================================
    // Callback DSL Tests
    // ============================================

    @Test
    fun `callback DSL should create with single path item`() {
        val result = callback {
            "{${'$'}request.body#/callbackUrl}" to {
                post {
                    summary = "Webhook notification"
                    responses {
                        ok { description = "Received" }
                    }
                }
            }
        }

        assertEquals(1, result.size)
        assertNotNull(result["{${'$'}request.body#/callbackUrl}"]?.post)
    }

    @Test
    fun `callback DSL should accept pre-built path items`() {
        val preBuilt = PathItem(
            post = Operation(
                responses = mapOf("200" to Response(description = "OK"))
            )
        )

        val result = callback {
            "{${'$'}request.body#/url}" to preBuilt
        }

        assertEquals(1, result.size)
        assertNotNull(result["{${'$'}request.body#/url}"])
    }

    @Test
    fun `callbacks DSL should create map of callbacks`() {
        val result = callbacks {
            "onSuccess" to {
                "{${'$'}request.body#/successUrl}" to {
                    post {
                        responses { ok { description = "OK" } }
                    }
                }
            }
            "onFailure" to {
                "{${'$'}request.body#/failureUrl}" to {
                    post {
                        responses { ok { description = "OK" } }
                    }
                }
            }
        }

        assertEquals(2, result.size)
        assertTrue(result.containsKey("onSuccess"))
        assertTrue(result.containsKey("onFailure"))
    }

    @Test
    fun `operation DSL should support callbacks`() {
        val result = operation {
            summary = "Async operation"
            callbacks {
                "onComplete" to {
                    "{${'$'}request.body#/webhook}" to {
                        post {
                            responses { ok { description = "Webhook received" } }
                        }
                    }
                }
            }
            responses {
                ok { description = "Accepted" }
            }
        }

        assertEquals(1, result.callbacks?.size)
        assertNotNull(result.callbacks?.get("onComplete"))
    }

    // ============================================
    // PathItem DSL Tests
    // ============================================

    @Test
    fun `pathItem DSL should create with single operation`() {
        val result = pathItem {
            get {
                summary = "Get resource"
                responses {
                    ok { description = "Success" }
                }
            }
        }

        assertNotNull(result.get)
        assertNull(result.post)
        assertEquals("Get resource", result.get?.summary)
    }

    @Test
    fun `pathItem DSL should create with multiple operations`() {
        val result = pathItem {
            get {
                summary = "Get"
                responses { ok { description = "OK" } }
            }
            post {
                summary = "Create"
                responses { created { description = "Created" } }
            }
            put {
                summary = "Update"
                responses { ok { description = "OK" } }
            }
            delete {
                summary = "Delete"
                responses { noContent { description = "Deleted" } }
            }
        }

        assertNotNull(result.get)
        assertNotNull(result.post)
        assertNotNull(result.put)
        assertNotNull(result.delete)
    }

    @Test
    fun `pathItem DSL should support all HTTP methods`() {
        val result = pathItem {
            get { responses { ok { description = "OK" } } }
            put { responses { ok { description = "OK" } } }
            post { responses { ok { description = "OK" } } }
            delete { responses { ok { description = "OK" } } }
            options { responses { ok { description = "OK" } } }
            head { responses { ok { description = "OK" } } }
            patch { responses { ok { description = "OK" } } }
            trace { responses { ok { description = "OK" } } }
            query { responses { ok { description = "OK" } } }
        }

        assertNotNull(result.get)
        assertNotNull(result.put)
        assertNotNull(result.post)
        assertNotNull(result.delete)
        assertNotNull(result.options)
        assertNotNull(result.head)
        assertNotNull(result.patch)
        assertNotNull(result.trace)
        assertNotNull(result.query)
    }

    @Test
    fun `pathItem DSL should support path-level properties`() {
        val result = pathItem {
            summary = "User operations"
            description = "Operations for managing users"
            ref = "#/paths/~1users"

            get {
                responses { ok { description = "OK" } }
            }
        }

        assertEquals("User operations", result.summary)
        assertEquals("Operations for managing users", result.description)
        assertEquals("#/paths/~1users", result.ref)
    }

    @Test
    fun `pathItem DSL should support path-level parameters`() {
        val result = pathItem {
            parameters {
                pathParameter("userId") { format = "uuid" }
            }

            get {
                responses { ok { description = "OK" } }
            }
            delete {
                responses { noContent { description = "Deleted" } }
            }
        }

        assertEquals(1, result.parameters?.size)
    }

    @Test
    fun `pathItem DSL should support path-level servers`() {
        val result = pathItem {
            servers {
                server { url = "https://special.example.com" }
            }
            get {
                responses { ok { description = "OK" } }
            }
        }

        assertEquals(1, result.servers?.size)
    }

    @Test
    fun `pathItem DSL should support additional operations`() {
        val result = pathItem {
            additionalOperations {
                "CUSTOM" to {
                    summary = "Custom method"
                    responses { ok { description = "OK" } }
                }
            }
        }

        assertEquals(1, result.additionalOperations?.size)
        assertNotNull(result.additionalOperations?.get("CUSTOM"))
    }

    @Test
    fun `pathItem DSL should serialize correctly`() {
        val result = pathItem {
            summary = "Test path"
            get {
                operationId = "testGet"
                responses { ok { description = "Success" } }
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"summary\":\"Test path\""))
        assertTrue(json.contains("\"get\""))
        assertTrue(json.contains("\"operationId\":\"testGet\""))
    }

    // ============================================
    // Paths Container DSL Tests
    // ============================================

    @Test
    fun `paths DSL should create with single path`() {
        val result = paths {
            "/users" to {
                get {
                    responses { ok { description = "User list" } }
                }
            }
        }

        assertEquals(1, result.size)
        assertTrue(result.containsKey("/users"))
    }

    @Test
    fun `paths DSL should create with multiple paths`() {
        val result = paths {
            "/users" to {
                get { responses { ok { description = "List" } } }
                post { responses { created { description = "Created" } } }
            }
            "/users/{userId}" to {
                parameters {
                    pathParameter("userId") { format = "uuid" }
                }
                get { responses { ok { description = "User" } } }
                put { responses { ok { description = "Updated" } } }
                delete { responses { noContent { description = "Deleted" } } }
            }
        }

        assertEquals(2, result.size)
        assertTrue(result.containsKey("/users"))
        assertTrue(result.containsKey("/users/{userId}"))
    }

    @Test
    fun `paths DSL should accept pre-built path items`() {
        val preBuilt = PathItem(
            get = Operation(
                responses = mapOf("200" to Response(description = "OK"))
            )
        )

        val result = paths {
            "/test" to preBuilt
        }

        assertEquals(1, result.size)
        assertNotNull(result["/test"]?.get)
    }

    @Test
    fun `paths DSL should fail for path not starting with slash`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            paths {
                "users" to {
                    get { responses { ok { description = "OK" } } }
                }
            }
        }

        assertTrue(exception.message?.contains("must start with a forward slash") == true)
    }

    @Test
    fun `paths DSL should serialize correctly`() {
        val result = paths {
            "/api/v1/users" to {
                get {
                    operationId = "listUsers"
                    responses { ok { description = "Users" } }
                }
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"/api/v1/users\""))
        assertTrue(json.contains("\"listUsers\""))
    }

    // ============================================
    // Integration Tests
    // ============================================

    @Test
    fun `complete API paths example`() {
        val result = paths {
            "/pets" to {
                get {
                    summary = "List all pets"
                    operationId = "listPets"
                    tags("pets")
                    parameters {
                        queryParameter("limit", "Maximum items to return") {
                            type = "integer"
                            format = "int32"
                        }
                    }
                    responses {
                        ok {
                            description = "A list of pets"
                            content {
                                "application/json" to {
                                    schema {
                                        type = "array"
                                        items = objectSchema {
                                            properties {
                                                "id" to integerSchema()
                                                "name" to stringSchema()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        default {
                            description = "Unexpected error"
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
                                    type = "object"
                                    properties {
                                        "name" to stringSchema()
                                        "tag" to stringSchema()
                                    }
                                }
                            }
                        }
                    }
                    responses {
                        created {
                            description = "Pet created"
                        }
                        badRequest {
                            description = "Invalid input"
                        }
                    }
                }
            }

            "/pets/{petId}" to {
                parameters {
                    pathParameter("petId", "The pet ID") {
                        type = "integer"
                        format = "int64"
                    }
                }
                get {
                    summary = "Get a pet by ID"
                    operationId = "getPetById"
                    tags("pets")
                    responses {
                        ok {
                            description = "Pet details"
                            content {
                                "application/json" to {
                                    schema { type = "object" }
                                }
                            }
                        }
                        notFound {
                            description = "Pet not found"
                        }
                    }
                }
                delete {
                    summary = "Delete a pet"
                    operationId = "deletePet"
                    tags("pets")
                    security {
                        requirement("api_key")
                    }
                    responses {
                        noContent {
                            description = "Pet deleted"
                        }
                        notFound {
                            description = "Pet not found"
                        }
                    }
                }
            }
        }

        // Verify structure
        assertEquals(2, result.size)

        // Verify /pets path
        val petsPath = result["/pets"]!!
        assertNotNull(petsPath.get)
        assertNotNull(petsPath.post)
        assertEquals("listPets", petsPath.get?.operationId)
        assertEquals("createPet", petsPath.post?.operationId)
        assertEquals(1, petsPath.get?.parameters?.size)

        // Verify /pets/{petId} path
        val petByIdPath = result["/pets/{petId}"]!!
        assertEquals(1, petByIdPath.parameters?.size)
        assertNotNull(petByIdPath.get)
        assertNotNull(petByIdPath.delete)
        assertEquals("getPetById", petByIdPath.get?.operationId)
        assertEquals("deletePet", petByIdPath.delete?.operationId)
        assertEquals(1, petByIdPath.delete?.security?.size)
    }

    @Test
    fun `operation with callbacks example`() {
        val result = operation {
            summary = "Subscribe to events"
            operationId = "subscribe"
            requestBody {
                required = true
                content {
                    "application/json" to {
                        schema {
                            type = "object"
                            properties {
                                "callbackUrl" to stringSchema { format = "uri" }
                                "eventTypes" to arraySchema {
                                    items = stringSchema()
                                }
                            }
                        }
                    }
                }
            }
            callbacks {
                "onEvent" to {
                    "{${'$'}request.body#/callbackUrl}" to {
                        post {
                            summary = "Event notification"
                            requestBody {
                                content {
                                    "application/json" to {
                                        schema {
                                            type = "object"
                                            properties {
                                                "eventType" to stringSchema()
                                                "data" to objectSchema()
                                            }
                                        }
                                    }
                                }
                            }
                            responses {
                                ok { description = "Event received" }
                            }
                        }
                    }
                }
            }
            responses {
                created {
                    description = "Subscription created"
                    headers {
                        "X-Subscription-Id" to {
                            schema { type = "string" }
                        }
                    }
                }
            }
        }

        assertNotNull(result.requestBody)
        assertEquals(1, result.callbacks?.size)
        val callback = result.callbacks?.get("onEvent")!!
        val pathItem = callback["{${'$'}request.body#/callbackUrl}"]!!
        assertNotNull(pathItem.post)
    }
}
