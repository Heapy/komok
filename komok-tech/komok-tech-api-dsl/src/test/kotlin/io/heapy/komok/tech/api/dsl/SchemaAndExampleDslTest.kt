package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SchemaAndExampleDslTest {

    // Schema DSL Tests

    @Test
    fun `schema DSL should create simple string schema`() {
        val result = schema {
            type = "string"
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"string"}""", json)
    }

    @Test
    fun `schema DSL should create string schema with constraints`() {
        val result = schema {
            type = "string"
            minLength = 1
            maxLength = 100
            pattern = "^[a-z]+$"
            format = "email"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"string","format":"email","minLength":1,"maxLength":100,"pattern":"^[a-z]+$"}""",
            json
        )
    }

    @Test
    fun `schema DSL should create integer schema with constraints`() {
        val result = schema {
            type = "integer"
            minimum = 0
            maximum = 100
            multipleOf = 5
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"integer","minimum":0,"maximum":100,"multipleOf":5}""",
            json
        )
    }

    @Test
    fun `schema DSL should create object schema with properties`() {
        val result = schema {
            type = "object"
            required("name", "email")
            properties {
                "name" to { type = "string"; minLength = 1 }
                "email" to { type = "string"; format = "email" }
                "age" to { type = "integer"; minimum = 0 }
            }
        }
        val json = compactJson.encodeToString(result)

        val expected = """{"type":"object","required":["name","email"],"properties":{"name":{"type":"string","minLength":1},"email":{"type":"string","format":"email"},"age":{"type":"integer","minimum":0}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `schema DSL should create array schema`() {
        val result = schema {
            type = "array"
            items = stringSchema()
            minItems = 1
            maxItems = 10
            uniqueItems = true
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"array","items":{"type":"string"},"minItems":1,"maxItems":10,"uniqueItems":true}""",
            json
        )
    }

    @Test
    fun `schema DSL should create reference schema`() {
        val result = schema {
            ref = "#/components/schemas/User"
        }
        val json = compactJson.encodeToString(result)

        assertEquals($$"""{"$ref":"#/components/schemas/User"}""", json)
    }

    @Test
    fun `schema DSL should create schema with enum`() {
        val result = schema {
            type = "string"
            enum("active", "inactive", "pending")
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"string","enum":["active","inactive","pending"]}""",
            json
        )
    }

    @Test
    fun `schema DSL should create schema with composition`() {
        val result = schema {
            allOf = listOf(
                refSchema("#/components/schemas/Base"),
                objectSchema {
                    properties {
                        "extra" to { type = "string" }
                    }
                }
            )
        }
        val json = compactJson.encodeToString(result)

        val expected = $$"""{"allOf":[{"$ref":"#/components/schemas/Base"},{"type":"object","properties":{"extra":{"type":"string"}}}]}"""
        assertEquals(expected, json)
    }

    @Test
    fun `schema DSL should create nullable schema`() {
        val result = schema {
            type = "string"
            nullable = true
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"string","nullable":true}""", json)
    }

    @Test
    fun `stringSchema helper should create string type`() {
        val result = stringSchema {
            minLength = 5
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"string","minLength":5}""", json)
    }

    @Test
    fun `integerSchema helper should create integer type`() {
        val result = integerSchema {
            minimum = 0
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"integer","minimum":0}""", json)
    }

    @Test
    fun `numberSchema helper should create number type`() {
        val result = numberSchema {
            minimum = 0.0
            maximum = 1.0
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"number","minimum":0.0,"maximum":1.0}""", json)
    }

    @Test
    fun `booleanTypeSchema helper should create boolean type`() {
        val result = booleanTypeSchema()
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"boolean"}""", json)
    }

    @Test
    fun `arraySchema helper should create array type`() {
        val result = arraySchema {
            items = integerSchema()
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"array","items":{"type":"integer"}}""", json)
    }

    @Test
    fun `objectSchema helper should create object type`() {
        val result = objectSchema {
            required("id")
            properties {
                "id" to integerSchema()
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"object","required":["id"],"properties":{"id":{"type":"integer"}}}""",
            json
        )
    }

    @Test
    fun `booleanSchema helper should create boolean literal schema`() {
        val trueSchema = booleanSchema(true)
        val falseSchema = booleanSchema(false)

        assertEquals("true", compactJson.encodeToString(trueSchema))
        assertEquals("false", compactJson.encodeToString(falseSchema))
    }

    @Test
    fun `refSchema helper should create reference schema`() {
        val result = refSchema("#/components/schemas/Pet")
        val json = compactJson.encodeToString(result)

        assertEquals($$"""{"$ref":"#/components/schemas/Pet"}""", json)
    }

    @Test
    fun `schema from JsonElement should work`() {
        val jsonElement = buildJsonObject {
            put("type", "string")
            put("format", "uuid")
        }
        val result = schema(jsonElement)
        val json = compactJson.encodeToString(result)

        assertEquals("""{"type":"string","format":"uuid"}""", json)
    }

    @Test
    fun `schemaProperties DSL should create property map`() {
        val result = schemaProperties {
            "name" to { type = "string" }
            "age" to integerSchema { minimum = 0 }
        }

        assertEquals(2, result.size)
        assertEquals("""{"type":"string"}""", compactJson.encodeToString(result["name"]))
        assertEquals("""{"type":"integer","minimum":0}""", compactJson.encodeToString(result["age"]))
    }

    @Test
    fun `schema DSL should round-trip correctly`() {
        val result = objectSchema {
            required("id", "name")
            properties {
                "id" to integerSchema()
                "name" to stringSchema { minLength = 1 }
                "tags" to arraySchema { items = stringSchema() }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Schema.serializer(), result)
    }

    // Example DSL Tests

    @Test
    fun `example DSL should create with value`() {
        val result = example {
            summary = "Test example"
            value = JsonPrimitive("test value")
        }

        assertEquals(
            Example(
                summary = "Test example",
                value = JsonPrimitive("test value")
            ),
            result
        )
    }

    @Test
    fun `example DSL should create with all properties`() {
        val result = example {
            summary = "User example"
            description = "An example user object"
            value = buildJsonObject {
                put("name", "John")
                put("age", 30)
            }
            extensions = mapOf("x-internal" to JsonPrimitive(true))
        }

        assertEquals("User example", result.summary)
        assertEquals("An example user object", result.description)
        assertEquals(mapOf("x-internal" to JsonPrimitive(true)), result.extensions)
    }

    @Test
    fun `example DSL should support value builder`() {
        val result = example {
            summary = "User"
            value {
                put("name", "John")
                put("email", "john@example.com")
                putJsonObject("address") {
                    put("city", "NYC")
                    put("zip", "10001")
                }
                putJsonArray("roles") {
                    add("admin")
                    add("user")
                }
            }
        }
        val json = compactJson.encodeToString(result)

        val expected = """{"summary":"User","value":{"name":"John","email":"john@example.com","address":{"city":"NYC","zip":"10001"},"roles":["admin","user"]}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `example DSL should create with externalValue`() {
        val result = example {
            summary = "External example"
            externalValue = "https://example.com/examples/user.json"
        }

        assertEquals(
            Example(
                summary = "External example",
                externalValue = "https://example.com/examples/user.json"
            ),
            result
        )
    }

    @Test
    fun `example DSL should create with serializedValue`() {
        val result = example {
            serializedValue = """{"key":"value"}"""
        }

        assertEquals(
            Example(serializedValue = """{"key":"value"}"""),
            result
        )
    }

    @Test
    fun `example DSL should create with dataValue`() {
        val result = example {
            dataValue = JsonPrimitive("data")
        }

        assertEquals(
            Example(dataValue = JsonPrimitive("data")),
            result
        )
    }

    @Test
    fun `example DSL should support dataValue builder`() {
        val result = example {
            dataValue {
                put("id", 123)
                put("status", "active")
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"dataValue":{"id":123,"status":"active"}}""", json)
    }

    @Test
    fun `example DSL should fail when value and externalValue both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            example {
                value = JsonPrimitive("test")
                externalValue = "https://example.com/test.json"
            }
        }

        assertEquals(
            "Example 'value' and 'externalValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `example DSL should fail when value and dataValue both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            example {
                value = JsonPrimitive("test")
                dataValue = JsonPrimitive("data")
            }
        }

        assertEquals(
            "Example 'value' and 'dataValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `example DSL should fail when value and serializedValue both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            example {
                value = JsonPrimitive("test")
                serializedValue = "serialized"
            }
        }

        assertEquals(
            "Example 'value' and 'serializedValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `example DSL should fail when serializedValue and externalValue both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            example {
                serializedValue = "serialized"
                externalValue = "https://example.com/test.json"
            }
        }

        assertEquals(
            "Example 'serializedValue' and 'externalValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `example DSL should allow dataValue and serializedValue together`() {
        val result = example {
            dataValue = JsonPrimitive("data")
            serializedValue = "serialized"
        }

        assertEquals(JsonPrimitive("data"), result.dataValue)
        assertEquals("serialized", result.serializedValue)
    }

    @Test
    fun `example DSL should serialize correctly`() {
        val result = example {
            summary = "Test"
            value = JsonPrimitive("hello")
        }
        val json = compactJson.encodeToString(result)

        assertEquals("""{"summary":"Test","value":"hello"}""", json)
    }

    @Test
    fun `example DSL should round-trip correctly`() {
        val result = example {
            summary = "User example"
            description = "An example user"
            value = buildJsonObject {
                put("name", "John")
                put("email", "john@example.com")
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Example.serializer(), result)
    }

    // Examples (map) DSL Tests

    @Test
    fun `examples DSL should create map of examples`() {
        val result = examples {
            "user" to {
                summary = "User example"
                value = JsonPrimitive("john")
            }
            "admin" to {
                summary = "Admin example"
                value = JsonPrimitive("admin")
            }
        }

        assertEquals(2, result.size)
        assertEquals("User example", result["user"]?.summary)
        assertEquals("Admin example", result["admin"]?.summary)
    }

    @Test
    fun `examples DSL should create empty map`() {
        val result = examples {}

        assertEquals(emptyMap<String, Example>(), result)
    }

    @Test
    fun `examples DSL should accept pre-built examples`() {
        val preBuilt = Example(summary = "Pre-built", value = JsonPrimitive("test"))

        val result = examples {
            "custom" to preBuilt
            "dsl" to {
                summary = "DSL built"
                value = JsonPrimitive("dsl")
            }
        }

        assertEquals(2, result.size)
        assertEquals("Pre-built", result["custom"]?.summary)
        assertEquals("DSL built", result["dsl"]?.summary)
    }

    @Test
    fun `examples DSL should fail if any example has mutual exclusivity violation`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            examples {
                "valid" to {
                    value = JsonPrimitive("ok")
                }
                "invalid" to {
                    value = JsonPrimitive("test")
                    externalValue = "https://example.com"
                }
            }
        }

        assertEquals(
            "Example 'value' and 'externalValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }
}
