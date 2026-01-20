package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParameterAndHeaderDslTest {

    // ============================================
    // MediaType DSL Tests
    // ============================================

    @Test
    fun `mediaType DSL should create MediaType with schema`() {
        val result = mediaType {
            schema {
                type = "string"
            }
        }

        assertEquals(
            MediaType(schema = schema { type = "string" }),
            result
        )
    }

    @Test
    fun `mediaType DSL should create MediaType with description and schema`() {
        val result = mediaType {
            description = "User object"
            schema {
                type = "object"
            }
        }

        assertEquals("User object", result.description)
        assertEquals("""{"type":"object"}""", compactJson.encodeToString(result.schema))
    }

    @Test
    fun `mediaType DSL should support nested examples DSL`() {
        val result = mediaType {
            schema { type = "string" }
            examples {
                "example1" to {
                    summary = "First example"
                    value = JsonPrimitive("hello")
                }
            }
        }

        assertEquals(1, result.examples?.size)
        assertEquals("First example", result.examples?.get("example1")?.summary)
    }

    @Test
    fun `mediaType DSL should fail when both example and examples are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            mediaType {
                schema { type = "string" }
                example = JsonPrimitive("test")
                examples {
                    "ex1" to { value = JsonPrimitive("other") }
                }
            }
        }

        assertEquals("MediaType 'example' and 'examples' are mutually exclusive", exception.message)
    }

    @Test
    fun `mediaType DSL should serialize correctly`() {
        val result = mediaType {
            schema { type = "string" }
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"schema":{"type":"string"}}""", json)
    }

    @Test
    fun `mediaType DSL should round-trip correctly`() {
        val result = mediaType {
            description = "Test media type"
            schema { type = "integer" }
        }

        TestHelpers.testRoundTripWithoutValidation(MediaType.serializer(), result)
    }

    // ============================================
    // Content DSL Tests
    // ============================================

    @Test
    fun `content DSL should create Content with single media type`() {
        val result = content {
            "application/json" to {
                schema { type = "object" }
            }
        }

        assertEquals(1, result.size)
        assertEquals("""{"type":"object"}""", compactJson.encodeToString(result["application/json"]?.schema))
    }

    @Test
    fun `content DSL should create Content with multiple media types`() {
        val result = content {
            "application/json" to {
                schema { type = "object" }
            }
            "text/plain" to {
                schema { type = "string" }
            }
        }

        assertEquals(2, result.size)
        assertEquals("""{"type":"object"}""", compactJson.encodeToString(result["application/json"]?.schema))
        assertEquals("""{"type":"string"}""", compactJson.encodeToString(result["text/plain"]?.schema))
    }

    @Test
    fun `content DSL should accept pre-built MediaType`() {
        val preBuiltMediaType = MediaType(schema = schema { type = "boolean" })

        val result = content {
            "application/json" to preBuiltMediaType
        }

        assertEquals(preBuiltMediaType, result["application/json"])
    }

    // ============================================
    // Parameter DSL Tests
    // ============================================

    @Test
    fun `parameter DSL should create path parameter with schema`() {
        val result = parameter {
            name = "userId"
            location = ParameterLocation.PATH
            required = true
            schema { type = "string" }
        }

        assertEquals(
            Parameter(
                name = "userId",
                location = ParameterLocation.PATH,
                required = true,
                schema = schema { type = "string" }
            ),
            result
        )
    }

    @Test
    fun `parameter DSL should create query parameter with all properties`() {
        val result = parameter {
            name = "filter"
            location = ParameterLocation.QUERY
            description = "Filter criteria"
            required = false
            deprecated = true
            style = ParameterStyle.FORM
            explode = true
            allowEmptyValue = true
            schema { type = "string" }
        }

        assertEquals("filter", result.name)
        assertEquals(ParameterLocation.QUERY, result.location)
        assertEquals("Filter criteria", result.description)
        assertEquals(false, result.required)
        assertEquals(true, result.deprecated)
        assertEquals(ParameterStyle.FORM, result.style)
        assertEquals(true, result.explode)
        assertEquals(true, result.allowEmptyValue)
    }

    @Test
    fun `parameter DSL should support nested schema DSL`() {
        val result = parameter {
            name = "age"
            location = ParameterLocation.QUERY
            schema {
                type = "integer"
                minimum = 0
                maximum = 150
            }
        }

        assertEquals(
            """{"type":"integer","minimum":0,"maximum":150}""",
            compactJson.encodeToString(result.schema)
        )
    }

    @Test
    fun `parameter DSL should support nested content DSL`() {
        val result = parameter {
            name = "data"
            location = ParameterLocation.QUERYSTRING
            content {
                "application/json" to {
                    schema { type = "object" }
                }
            }
        }

        assertEquals(1, result.content?.size)
        assertEquals(
            """{"type":"object"}""",
            compactJson.encodeToString(result.content?.get("application/json")?.schema)
        )
    }

    @Test
    fun `parameter DSL should support nested examples DSL`() {
        val result = parameter {
            name = "status"
            location = ParameterLocation.QUERY
            schema { type = "string" }
            examples {
                "active" to {
                    summary = "Active status"
                    value = JsonPrimitive("active")
                }
                "inactive" to {
                    summary = "Inactive status"
                    value = JsonPrimitive("inactive")
                }
            }
        }

        assertEquals(2, result.examples?.size)
        assertEquals("Active status", result.examples?.get("active")?.summary)
        assertEquals("Inactive status", result.examples?.get("inactive")?.summary)
    }

    @Test
    fun `parameter DSL should fail when name is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                location = ParameterLocation.QUERY
                schema { type = "string" }
            }
        }

        assertEquals("Parameter name is required", exception.message)
    }

    @Test
    fun `parameter DSL should fail when location is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "test"
                schema { type = "string" }
            }
        }

        assertEquals("Parameter location is required", exception.message)
    }

    @Test
    fun `parameter DSL should fail when neither schema nor content is provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "test"
                location = ParameterLocation.QUERY
            }
        }

        assertEquals("Parameter must have exactly one of 'schema' or 'content' specified", exception.message)
    }

    @Test
    fun `parameter DSL should fail when both schema and content are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "test"
                location = ParameterLocation.QUERY
                schema { type = "string" }
                content {
                    "application/json" to { schema { type = "string" } }
                }
            }
        }

        assertEquals("Parameter must have exactly one of 'schema' or 'content' specified", exception.message)
    }

    @Test
    fun `parameter DSL should fail when path parameter is not required`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "id"
                location = ParameterLocation.PATH
                required = false
                schema { type = "string" }
            }
        }

        assertEquals("Parameter with location 'path' must have 'required' set to true", exception.message)
    }

    @Test
    fun `parameter DSL should fail when querystring uses schema instead of content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "data"
                location = ParameterLocation.QUERYSTRING
                schema { type = "object" }
            }
        }

        assertEquals("Parameter with location 'querystring' must use 'content' (not 'schema')", exception.message)
    }

    @Test
    fun `parameter DSL should fail when content has more than one entry`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "data"
                location = ParameterLocation.QUERY
                content {
                    "application/json" to { schema { type = "object" } }
                    "text/plain" to { schema { type = "string" } }
                }
            }
        }

        assertEquals("Parameter 'content' must have exactly one media type entry", exception.message)
    }

    @Test
    fun `parameter DSL should fail when both example and examples are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "test"
                location = ParameterLocation.QUERY
                schema { type = "string" }
                example = JsonPrimitive("test")
                examples {
                    "ex1" to { value = JsonPrimitive("other") }
                }
            }
        }

        assertEquals("Parameter 'example' and 'examples' are mutually exclusive", exception.message)
    }

    @Test
    fun `parameter DSL should fail when style is used with content`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            parameter {
                name = "data"
                location = ParameterLocation.QUERY
                style = ParameterStyle.FORM
                content {
                    "application/json" to { schema { type = "object" } }
                }
            }
        }

        assertEquals("Parameter properties 'style', 'explode', and 'allowReserved' can only be used with 'schema'", exception.message)
    }

    @Test
    fun `parameter DSL should fail when allowEmptyValue is used with non-query location`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            parameter {
                name = "id"
                location = ParameterLocation.PATH
                required = true
                allowEmptyValue = true
                schema { type = "string" }
            }
        }

        assertEquals("Parameter property 'allowEmptyValue' can only be used with location 'query'", exception.message)
    }

    @Test
    fun `parameter DSL should serialize correctly`() {
        val result = parameter {
            name = "userId"
            location = ParameterLocation.PATH
            required = true
            description = "User identifier"
            schema { type = "string" }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"name":"userId","in":"path","description":"User identifier","required":true,"schema":{"type":"string"}}""",
            json
        )
    }

    @Test
    fun `parameter DSL should round-trip correctly`() {
        val result = parameter {
            name = "filter"
            location = ParameterLocation.QUERY
            description = "Filter criteria"
            style = ParameterStyle.FORM
            explode = true
            schema { type = "string" }
        }

        TestHelpers.testRoundTripWithoutValidation(Parameter.serializer(), result)
    }

    // ============================================
    // Parameters (list) DSL Tests
    // ============================================

    @Test
    fun `parameters DSL should create list of parameters`() {
        val result = parameters {
            parameter {
                name = "userId"
                location = ParameterLocation.PATH
                required = true
                schema { type = "string" }
            }
            parameter {
                name = "limit"
                location = ParameterLocation.QUERY
                schema { type = "integer" }
            }
        }

        assertEquals(2, result.size)
        assertEquals("userId", result[0].name)
        assertEquals("limit", result[1].name)
    }

    @Test
    fun `parameters DSL should create empty list`() {
        val result = parameters {}

        assertEquals(emptyList<Parameter>(), result)
    }

    @Test
    fun `parameters DSL should accept pre-built parameters`() {
        val preBuiltParam = Parameter(
            name = "prebuilt",
            location = ParameterLocation.HEADER,
            schema = schema { type = "string" }
        )

        val result = parameters {
            parameter {
                name = "first"
                location = ParameterLocation.PATH
                required = true
                schema { type = "string" }
            }
            parameter(preBuiltParam)
        }

        assertEquals(2, result.size)
        assertEquals("first", result[0].name)
        assertEquals("prebuilt", result[1].name)
    }

    // ============================================
    // Header DSL Tests
    // ============================================

    @Test
    fun `header DSL should create Header with schema`() {
        val result = header {
            schema { type = "string" }
        }

        assertEquals(
            Header(schema = schema { type = "string" }),
            result
        )
    }

    @Test
    fun `header DSL should create Header with all properties`() {
        val result = header {
            description = "Rate limit remaining"
            required = true
            deprecated = false
            style = "simple"
            explode = false
            schema { type = "integer" }
        }

        assertEquals("Rate limit remaining", result.description)
        assertEquals(true, result.required)
        assertEquals(false, result.deprecated)
        assertEquals("simple", result.style)
        assertEquals(false, result.explode)
    }

    @Test
    fun `header DSL should support nested schema DSL`() {
        val result = header {
            schema {
                type = "integer"
                minimum = 0
            }
        }

        assertEquals(
            """{"type":"integer","minimum":0}""",
            compactJson.encodeToString(result.schema)
        )
    }

    @Test
    fun `header DSL should support nested content DSL`() {
        val result = header {
            content {
                "application/json" to {
                    schema { type = "object" }
                }
            }
        }

        assertEquals(1, result.content?.size)
        assertEquals(
            """{"type":"object"}""",
            compactJson.encodeToString(result.content?.get("application/json")?.schema)
        )
    }

    @Test
    fun `header DSL should support nested examples DSL`() {
        val result = header {
            schema { type = "string" }
            examples {
                "uuid" to {
                    summary = "UUID format"
                    value = JsonPrimitive("550e8400-e29b-41d4-a716-446655440000")
                }
            }
        }

        assertEquals(1, result.examples?.size)
        assertEquals("UUID format", result.examples?.get("uuid")?.summary)
    }

    @Test
    fun `header DSL should fail when neither schema nor content is provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            header {}
        }

        assertEquals("Header must have exactly one of 'schema' or 'content' specified", exception.message)
    }

    @Test
    fun `header DSL should fail when both schema and content are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            header {
                schema { type = "string" }
                content {
                    "text/plain" to { schema { type = "string" } }
                }
            }
        }

        assertEquals("Header must have exactly one of 'schema' or 'content' specified", exception.message)
    }

    @Test
    fun `header DSL should fail when both example and examples are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            header {
                schema { type = "string" }
                example = JsonPrimitive("test")
                examples {
                    "ex1" to { value = JsonPrimitive("other") }
                }
            }
        }

        assertEquals("Header 'example' and 'examples' are mutually exclusive", exception.message)
    }

    @Test
    fun `header DSL should serialize correctly`() {
        val result = header {
            description = "Request ID"
            required = true
            schema { type = "string" }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"description":"Request ID","required":true,"schema":{"type":"string"}}""",
            json
        )
    }

    @Test
    fun `header DSL should round-trip correctly`() {
        val result = header {
            description = "Custom header"
            required = false
            deprecated = true
            schema { type = "string" }
        }

        TestHelpers.testRoundTripWithoutValidation(Header.serializer(), result)
    }

    // ============================================
    // Headers (map) DSL Tests
    // ============================================

    @Test
    fun `headers DSL should create map of headers`() {
        val result = headers {
            "X-Rate-Limit-Remaining" to {
                description = "Number of requests remaining"
                schema { type = "integer" }
            }
            "X-Request-Id" to {
                description = "Unique request identifier"
                schema { type = "string" }
            }
        }

        assertEquals(2, result.size)
        assertEquals("Number of requests remaining", result["X-Rate-Limit-Remaining"]?.description)
        assertEquals("Unique request identifier", result["X-Request-Id"]?.description)
    }

    @Test
    fun `headers DSL should accept pre-built headers`() {
        val preBuiltHeader = Header(
            description = "Pre-built header",
            schema = schema { type = "boolean" }
        )

        val result = headers {
            "X-Custom" to {
                schema { type = "string" }
            }
            "X-PreBuilt" to preBuiltHeader
        }

        assertEquals(2, result.size)
        assertEquals("Pre-built header", result["X-PreBuilt"]?.description)
    }

    // ============================================
    // Convenience Functions Tests
    // ============================================

    @Test
    fun `pathParameter should create path parameter with correct defaults`() {
        val result = pathParameter("userId") {
            type = "string"
            format = "uuid"
        }

        assertEquals("userId", result.name)
        assertEquals(ParameterLocation.PATH, result.location)
        assertEquals(true, result.required)
        assertEquals(
            """{"type":"string","format":"uuid"}""",
            compactJson.encodeToString(result.schema)
        )
    }

    @Test
    fun `pathParameter should accept description`() {
        val result = pathParameter("id", description = "Resource identifier") {
            type = "integer"
        }

        assertEquals("Resource identifier", result.description)
    }

    @Test
    fun `queryParameter should create query parameter with correct defaults`() {
        val result = queryParameter("filter", description = "Filter criteria") {
            type = "string"
        }

        assertEquals("filter", result.name)
        assertEquals(ParameterLocation.QUERY, result.location)
        assertEquals(false, result.required)
        assertEquals("Filter criteria", result.description)
    }

    @Test
    fun `queryParameter should support required flag`() {
        val result = queryParameter("search", required = true) {
            type = "string"
        }

        assertEquals(true, result.required)
    }

    @Test
    fun `headerParameter should create header parameter with correct defaults`() {
        val result = headerParameter("X-Api-Key", description = "API Key", required = true) {
            type = "string"
        }

        assertEquals("X-Api-Key", result.name)
        assertEquals(ParameterLocation.HEADER, result.location)
        assertEquals(true, result.required)
        assertEquals("API Key", result.description)
    }

    @Test
    fun `cookieParameter should create cookie parameter with correct defaults`() {
        val result = cookieParameter("session", description = "Session ID") {
            type = "string"
        }

        assertEquals("session", result.name)
        assertEquals(ParameterLocation.COOKIE, result.location)
        assertEquals(false, result.required)
        assertEquals("Session ID", result.description)
    }

    // ============================================
    // Complex Scenarios Tests
    // ============================================

    @Test
    fun `should create parameter with complex schema`() {
        val result = parameter {
            name = "tags"
            location = ParameterLocation.QUERY
            description = "Tags to filter by"
            style = ParameterStyle.FORM
            explode = true
            schema {
                type = "array"
                items = stringSchema { minLength = 1 }
                minItems = 1
                uniqueItems = true
            }
        }

        assertEquals("tags", result.name)
        assertEquals(ParameterStyle.FORM, result.style)
        assertEquals(
            """{"type":"array","items":{"type":"string","minLength":1},"minItems":1,"uniqueItems":true}""",
            compactJson.encodeToString(result.schema)
        )
    }

    @Test
    fun `should create parameter with extensions`() {
        val result = parameter {
            name = "custom"
            location = ParameterLocation.QUERY
            schema { type = "string" }
            extensions = mapOf(
                "x-custom-field" to JsonPrimitive("custom-value"),
                "x-deprecated-reason" to JsonPrimitive("Use 'newCustom' instead")
            )
        }

        assertEquals(2, result.extensions?.size)
        assertEquals(JsonPrimitive("custom-value"), result.extensions?.get("x-custom-field"))
    }

    @Test
    fun `should create header with extensions`() {
        val result = header {
            description = "Custom header"
            schema { type = "string" }
            extensions = mapOf("x-internal" to JsonPrimitive(true))
        }

        assertEquals(1, result.extensions?.size)
        assertEquals(JsonPrimitive(true), result.extensions?.get("x-internal"))
    }

    @Test
    fun `should create complete API parameter set`() {
        val result = parameters {
            parameter {
                name = "petId"
                location = ParameterLocation.PATH
                required = true
                description = "ID of pet to return"
                schema {
                    type = "integer"
                    format = "int64"
                }
            }
            parameter {
                name = "status"
                location = ParameterLocation.QUERY
                description = "Status values that need to be considered for filter"
                style = ParameterStyle.FORM
                explode = true
                schema {
                    type = "array"
                    items = stringSchema {
                        enum("available", "pending", "sold")
                    }
                }
            }
            parameter {
                name = "api_key"
                location = ParameterLocation.HEADER
                required = true
                schema { type = "string" }
            }
        }

        assertEquals(3, result.size)
        assertEquals("petId", result[0].name)
        assertEquals("status", result[1].name)
        assertEquals("api_key", result[2].name)
    }
}
