package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RequestAndResponseDslTest {

    // ============================================
    // RequestBody DSL Tests
    // ============================================

    @Test
    fun `requestBody DSL should create with minimal content`() {
        val result = requestBody {
            content {
                "application/json" to {
                    schema {
                        type = "object"
                    }
                }
            }
        }

        assertEquals(
            RequestBody(
                content = mapOf(
                    "application/json" to MediaType(
                        schema = Schema(buildJsonObject { put("type", "object") })
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `requestBody DSL should create with all properties`() {
        val result = requestBody {
            description = "User object to create"
            required = true
            content {
                "application/json" to {
                    schema {
                        type = "object"
                        properties {
                            "name" to stringSchema()
                            "email" to stringSchema { format = "email" }
                        }
                    }
                }
            }
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertEquals("User object to create", result.description)
        assertEquals(true, result.required)
        assertEquals(1, result.content.size)
        assertTrue(result.content.containsKey("application/json"))
        assertEquals(mapOf("x-custom" to JsonPrimitive("value")), result.extensions)
    }

    @Test
    fun `requestBody DSL should support multiple content types`() {
        val result = requestBody {
            content {
                "application/json" to {
                    schema { type = "object" }
                }
                "application/xml" to {
                    schema { type = "object" }
                }
                "text/plain" to {
                    schema { type = "string" }
                }
            }
        }

        assertEquals(3, result.content.size)
        assertTrue(result.content.containsKey("application/json"))
        assertTrue(result.content.containsKey("application/xml"))
        assertTrue(result.content.containsKey("text/plain"))
    }

    @Test
    fun `requestBody DSL should fail when content is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            requestBody {
                description = "Missing content"
            }
        }

        assertEquals("RequestBody content is required", exception.message)
    }

    @Test
    fun `requestBody DSL should fail when content is empty`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            requestBody {
                content {}
            }
        }

        assertEquals("RequestBody 'content' must not be empty", exception.message)
    }

    @Test
    fun `requestBody DSL should serialize correctly`() {
        val result = requestBody {
            description = "Create user"
            required = true
            content {
                "application/json" to {
                    schema { type = "object" }
                }
            }
        }
        val json = compactJson.encodeToString(result)

        val expected = """{"content":{"application/json":{"schema":{"type":"object"}}},"description":"Create user","required":true}"""
        assertEquals(expected, json)
    }

    @Test
    fun `requestBody DSL should round-trip correctly`() {
        val result = requestBody {
            description = "User data"
            required = true
            content {
                "application/json" to {
                    schema {
                        type = "object"
                        properties {
                            "name" to stringSchema()
                        }
                    }
                }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(RequestBody.serializer(), result)
    }

    @Test
    fun `requestBody DSL should support content with encoding`() {
        val result = requestBody {
            content {
                "multipart/form-data" to {
                    schema {
                        type = "object"
                        properties {
                            "file" to schema {
                                type = "string"
                                format = "binary"
                            }
                        }
                    }
                    encoding {
                        "file" to {
                            contentType = "application/octet-stream"
                        }
                    }
                }
            }
        }

        val mediaType = result.content["multipart/form-data"]!!
        assertEquals("application/octet-stream", mediaType.encoding?.get("file")?.contentType)
    }

    // ============================================
    // Response DSL Tests
    // ============================================

    @Test
    fun `response DSL should create with description only`() {
        val result = response {
            description = "Successful response"
        }

        assertEquals(
            Response(description = "Successful response"),
            result
        )
    }

    @Test
    fun `response DSL should create with all properties`() {
        val result = response {
            description = "Successful response"
            summary = "Success"
            headers {
                "X-Rate-Limit" to {
                    description = "Rate limit remaining"
                    schema { type = "integer" }
                }
            }
            content {
                "application/json" to {
                    schema { type = "object" }
                }
            }
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertEquals("Successful response", result.description)
        assertEquals("Success", result.summary)
        assertEquals(1, result.headers?.size)
        assertEquals(1, result.content?.size)
        assertEquals(mapOf("x-custom" to JsonPrimitive("value")), result.extensions)
    }

    @Test
    fun `response DSL should support inline headers`() {
        val result = response {
            description = "Response with headers"
            headers {
                "X-Rate-Limit" to {
                    description = "Requests remaining"
                    schema { type = "integer" }
                }
                "X-Request-Id" to {
                    description = "Request identifier"
                    schema { type = "string" }
                }
            }
        }

        assertEquals(2, result.headers?.size)
        val rateLimitHeader = result.headers?.get("X-Rate-Limit")
        assertTrue(rateLimitHeader is Direct)
        assertEquals("Requests remaining", (rateLimitHeader as Direct).value.description)
    }

    @Test
    fun `response DSL should support header references`() {
        val result = response {
            description = "Response with header references"
            headers {
                "X-Rate-Limit" toRef "#/components/headers/RateLimit"
                "X-Request-Id" to {
                    schema { type = "string" }
                }
            }
        }

        assertEquals(2, result.headers?.size)
        val rateLimitHeader = result.headers?.get("X-Rate-Limit")
        assertTrue(rateLimitHeader is Reference)
        assertEquals("#/components/headers/RateLimit", (rateLimitHeader as Reference).ref)
    }

    @Test
    fun `response DSL should support header references with metadata`() {
        val result = response {
            description = "Response with detailed reference"
            headers {
                ref(
                    name = "X-Custom-Header",
                    ref = "#/components/headers/Custom",
                    summary = "Custom header",
                    description = "A custom header reference"
                )
            }
        }

        val header = result.headers?.get("X-Custom-Header")
        assertTrue(header is Reference)
        val ref = header as Reference
        assertEquals("#/components/headers/Custom", ref.ref)
        assertEquals("Custom header", ref.summary)
        assertEquals("A custom header reference", ref.description)
    }

    @Test
    fun `response DSL should support pre-built headers`() {
        val preBuiltHeader = Header(
            description = "Pre-built header",
            schema = Schema(buildJsonObject { put("type", "string") })
        )

        val result = response {
            description = "Response with pre-built header"
            headers {
                "X-Pre-Built" to preBuiltHeader
            }
        }

        val header = result.headers?.get("X-Pre-Built")
        assertTrue(header is Direct)
        assertEquals("Pre-built header", (header as Direct).value.description)
    }

    @Test
    fun `response DSL should support content with examples`() {
        val result = response {
            description = "Response with examples"
            content {
                "application/json" to {
                    schema { type = "object" }
                    examples {
                        "example1" to {
                            summary = "First example"
                            value = buildJsonObject {
                                put("id", 1)
                                put("name", "John")
                            }
                        }
                    }
                }
            }
        }

        val mediaType = result.content?.get("application/json")
        assertEquals(1, mediaType?.examples?.size)
        assertEquals("First example", mediaType?.examples?.get("example1")?.summary)
    }

    @Test
    fun `response DSL should serialize correctly`() {
        val result = response {
            description = "Success"
            content {
                "application/json" to {
                    schema { type = "object" }
                }
            }
        }
        val json = compactJson.encodeToString(result)

        val expected = """{"description":"Success","content":{"application/json":{"schema":{"type":"object"}}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `response DSL should serialize headers correctly`() {
        val result = response {
            description = "Response with headers"
            headers {
                "X-Rate-Limit" to {
                    schema { type = "integer" }
                }
                "X-Request-Id" toRef "#/components/headers/RequestId"
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"X-Rate-Limit\""))
        assertTrue(json.contains("\"X-Request-Id\""))
        assertTrue(json.contains("\"\$ref\":\"#/components/headers/RequestId\""))
    }

    @Test
    fun `response DSL should round-trip correctly`() {
        val result = response {
            description = "Round-trip test"
            summary = "Test"
            content {
                "application/json" to {
                    schema { type = "object" }
                }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Response.serializer(), result)
    }

    // ============================================
    // Responses Container DSL Tests
    // ============================================

    @Test
    fun `responses DSL should create with single response`() {
        val result = responses {
            "200" to {
                description = "Successful response"
            }
        }

        assertEquals(1, result.size)
        assertEquals("Successful response", result["200"]?.description)
    }

    @Test
    fun `responses DSL should create with multiple responses`() {
        val result = responses {
            "200" to {
                description = "Successful response"
            }
            "400" to {
                description = "Bad request"
            }
            "500" to {
                description = "Internal server error"
            }
        }

        assertEquals(3, result.size)
        assertEquals("Successful response", result["200"]?.description)
        assertEquals("Bad request", result["400"]?.description)
        assertEquals("Internal server error", result["500"]?.description)
    }

    @Test
    fun `responses DSL should support default response`() {
        val result = responses {
            "200" to {
                description = "Success"
            }
            default {
                description = "Unexpected error"
            }
        }

        assertEquals(2, result.size)
        assertEquals("Unexpected error", result["default"]?.description)
    }

    @Test
    fun `responses DSL should support wildcard status codes`() {
        val result = responses {
            "2XX" to {
                description = "Success responses"
            }
            "4XX" to {
                description = "Client errors"
            }
            "5XX" to {
                description = "Server errors"
            }
        }

        assertEquals(3, result.size)
        assertEquals("Success responses", result["2XX"]?.description)
        assertEquals("Client errors", result["4XX"]?.description)
        assertEquals("Server errors", result["5XX"]?.description)
    }

    @Test
    fun `responses DSL should support all valid wildcard patterns`() {
        val result = responses {
            "1XX" to { description = "Informational" }
            "2XX" to { description = "Success" }
            "3XX" to { description = "Redirection" }
            "4XX" to { description = "Client Error" }
            "5XX" to { description = "Server Error" }
        }

        assertEquals(5, result.size)
    }

    @Test
    fun `responses DSL should support convenience methods`() {
        val result = responses {
            ok {
                description = "OK"
            }
            created {
                description = "Created"
            }
            noContent {
                description = "No Content"
            }
            badRequest {
                description = "Bad Request"
            }
            unauthorized {
                description = "Unauthorized"
            }
            forbidden {
                description = "Forbidden"
            }
            notFound {
                description = "Not Found"
            }
            internalServerError {
                description = "Internal Server Error"
            }
        }

        assertEquals(8, result.size)
        assertEquals("OK", result["200"]?.description)
        assertEquals("Created", result["201"]?.description)
        assertEquals("No Content", result["204"]?.description)
        assertEquals("Bad Request", result["400"]?.description)
        assertEquals("Unauthorized", result["401"]?.description)
        assertEquals("Forbidden", result["403"]?.description)
        assertEquals("Not Found", result["404"]?.description)
        assertEquals("Internal Server Error", result["500"]?.description)
    }

    @Test
    fun `responses DSL should accept pre-built responses`() {
        val preBuilt = Response(description = "Pre-built response")

        val result = responses {
            "200" to preBuilt
            "404" to {
                description = "Not found"
            }
        }

        assertEquals(2, result.size)
        assertEquals("Pre-built response", result["200"]?.description)
    }

    @Test
    fun `responses DSL should accept pre-built default response`() {
        val preBuilt = Response(description = "Pre-built default")

        val result = responses {
            "200" to {
                description = "OK"
            }
            default(preBuilt)
        }

        assertEquals(2, result.size)
        assertEquals("Pre-built default", result["default"]?.description)
    }

    @Test
    fun `responses DSL should fail when empty`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses {}
        }

        assertEquals("Responses must contain at least one response", exception.message)
    }

    @Test
    fun `responses DSL should fail for invalid status code`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses {
                "999" to {
                    description = "Invalid"
                }
            }
        }

        assertTrue(exception.message?.contains("Invalid status code: '999'") == true)
    }

    @Test
    fun `responses DSL should fail for non-numeric status code`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses {
                "abc" to {
                    description = "Invalid"
                }
            }
        }

        assertTrue(exception.message?.contains("Invalid status code: 'abc'") == true)
    }

    @Test
    fun `responses DSL should fail for invalid wildcard pattern`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses {
                "6XX" to {
                    description = "Invalid wildcard"
                }
            }
        }

        assertTrue(exception.message?.contains("Invalid status code: '6XX'") == true)
    }

    @Test
    fun `responses DSL should fail for status code below 100`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            responses {
                "099" to {
                    description = "Invalid"
                }
            }
        }

        assertTrue(exception.message?.contains("Invalid status code") == true)
    }

    @Test
    fun `responses DSL should accept valid boundary status codes`() {
        val result = responses {
            "100" to { description = "Continue" }
            "599" to { description = "Network Connect Timeout" }
        }

        assertEquals(2, result.size)
    }

    @Test
    fun `responses DSL should serialize correctly`() {
        val result = responses {
            "200" to {
                description = "Success"
            }
            "400" to {
                description = "Bad request"
            }
        }
        val json = compactJson.encodeToString(result)

        assertTrue(json.contains("\"200\""))
        assertTrue(json.contains("\"400\""))
        assertTrue(json.contains("\"Success\""))
        assertTrue(json.contains("\"Bad request\""))
    }

    @Test
    fun `responses DSL should support complex response bodies`() {
        val result = responses {
            ok {
                description = "Successful operation"
                headers {
                    "X-Rate-Limit" to {
                        description = "Rate limit"
                        schema { type = "integer" }
                    }
                }
                content {
                    "application/json" to {
                        schema {
                            type = "object"
                            properties {
                                "id" to integerSchema()
                                "name" to stringSchema()
                            }
                        }
                        examples {
                            "example" to {
                                value = buildJsonObject {
                                    put("id", 1)
                                    put("name", "John")
                                }
                            }
                        }
                    }
                }
            }
            badRequest {
                description = "Validation error"
                content {
                    "application/json" to {
                        schema {
                            type = "object"
                            properties {
                                "error" to stringSchema()
                                "details" to arraySchema {
                                    items = stringSchema()
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals(2, result.size)
        val okResponse = result["200"]!!
        assertEquals(1, okResponse.headers?.size)
        assertEquals(1, okResponse.content?.size)
        assertEquals(1, okResponse.content?.get("application/json")?.examples?.size)
    }

    // ============================================
    // ReferenceableHeaders DSL Tests
    // ============================================

    @Test
    fun `referenceableHeaders DSL should create map with inline headers`() {
        val result = referenceableHeaders {
            "X-Rate-Limit" to {
                description = "Rate limit"
                schema { type = "integer" }
            }
        }

        assertEquals(1, result.size)
        assertTrue(result["X-Rate-Limit"] is Direct)
    }

    @Test
    fun `referenceableHeaders DSL should create map with references`() {
        val result = referenceableHeaders {
            "X-Rate-Limit" toRef "#/components/headers/RateLimit"
        }

        assertEquals(1, result.size)
        assertTrue(result["X-Rate-Limit"] is Reference)
        assertEquals("#/components/headers/RateLimit", (result["X-Rate-Limit"] as Reference).ref)
    }

    @Test
    fun `referenceableHeaders DSL should create mixed map`() {
        val result = referenceableHeaders {
            "X-Rate-Limit" to {
                schema { type = "integer" }
            }
            "X-Request-Id" toRef "#/components/headers/RequestId"
        }

        assertEquals(2, result.size)
        assertTrue(result["X-Rate-Limit"] is Direct)
        assertTrue(result["X-Request-Id"] is Reference)
    }

    @Test
    fun `referenceableHeaders DSL should create empty map`() {
        val result = referenceableHeaders {}

        assertEquals(0, result.size)
    }

    // ============================================
    // Integration Tests
    // ============================================

    @Test
    fun `full API operation example with request and responses`() {
        val requestBody = requestBody {
            description = "User to create"
            required = true
            content {
                "application/json" to {
                    schema {
                        type = "object"
                        required("name", "email")
                        properties {
                            "name" to stringSchema()
                            "email" to stringSchema { format = "email" }
                            "age" to integerSchema { minimum = 0 }
                        }
                    }
                    examples {
                        "john" to {
                            summary = "John Doe"
                            value = buildJsonObject {
                                put("name", "John Doe")
                                put("email", "john@example.com")
                                put("age", 30)
                            }
                        }
                    }
                }
            }
        }

        val apiResponses = responses {
            created {
                description = "User created successfully"
                headers {
                    "Location" to {
                        description = "URL of created resource"
                        schema { type = "string" }
                    }
                }
                content {
                    "application/json" to {
                        schema {
                            type = "object"
                            properties {
                                "id" to stringSchema { format = "uuid" }
                                "name" to stringSchema()
                                "email" to stringSchema()
                            }
                        }
                    }
                }
            }
            badRequest {
                description = "Invalid input"
                content {
                    "application/json" to {
                        schema {
                            type = "object"
                            properties {
                                "message" to stringSchema()
                                "errors" to arraySchema {
                                    items = stringSchema()
                                }
                            }
                        }
                    }
                }
            }
            unauthorized {
                description = "Authentication required"
            }
            internalServerError {
                description = "Unexpected error"
            }
        }

        // Verify request body
        assertEquals(true, requestBody.required)
        assertEquals(1, requestBody.content.size)

        // Verify responses
        assertEquals(4, apiResponses.size)
        assertTrue(apiResponses.containsKey("201"))
        assertTrue(apiResponses.containsKey("400"))
        assertTrue(apiResponses.containsKey("401"))
        assertTrue(apiResponses.containsKey("500"))

        // Verify created response has headers
        assertEquals(1, apiResponses["201"]?.headers?.size)
    }
}
