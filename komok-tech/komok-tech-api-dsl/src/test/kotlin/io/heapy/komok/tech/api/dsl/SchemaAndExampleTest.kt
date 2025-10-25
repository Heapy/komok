package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SchemaAndExampleTest {

    // Schema Tests

    @Test
    fun `should serialize boolean schema true`() {
        val schema = Schema(JsonPrimitive(true))
        val json = compactJson.encodeToString(schema)

        assertEquals("true", json)
    }

    @Test
    fun `should serialize boolean schema false`() {
        val schema = Schema(JsonPrimitive(false))
        val json = compactJson.encodeToString(schema)

        assertEquals("false", json)
    }

    @Test
    fun `should serialize simple object schema`() {
        val schemaObject = buildJsonObject {
            put("type", "string")
        }
        val schema = Schema(schemaObject)
        val json = compactJson.encodeToString(schema)

        assertEquals("""{"type":"string"}""", json)
    }

    @Test
    fun `should serialize complex object schema`() {
        val schemaObject = buildJsonObject {
            put("type", "object")
            put("required", buildJsonArray {
                add("name")
                add("email")
            })
            put("properties", buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                    put("minLength", 1)
                })
                put("email", buildJsonObject {
                    put("type", "string")
                    put("format", "email")
                })
                put("age", buildJsonObject {
                    put("type", "integer")
                    put("minimum", 0)
                })
            })
        }
        val schema = Schema(schemaObject)
        val json = compactJson.encodeToString(schema)

        val expected = """{"type":"object","required":["name","email"],"properties":{"name":{"type":"string","minLength":1},"email":{"type":"string","format":"email"},"age":{"type":"integer","minimum":0}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize schema with ref`() {
        val schemaObject = buildJsonObject {
            put($$"$ref", "#/components/schemas/User")
        }
        val schema = Schema(schemaObject)
        val json = compactJson.encodeToString(schema)

        assertEquals($$"""{"$ref":"#/components/schemas/User"}""", json)
    }

    @Test
    fun `should deserialize boolean schema`() {
        val json = "true"
        val schema = compactJson.decodeFromString<Schema>(json)

        assertEquals(JsonPrimitive(true), schema.schema)
    }

    @Test
    fun `should deserialize object schema`() {
        val json = """{"type":"string","maxLength":100}"""
        val schema = compactJson.decodeFromString<Schema>(json)

        val expected = buildJsonObject {
            put("type", "string")
            put("maxLength", 100)
        }
        assertEquals(expected, schema.schema)
    }

    @Test
    fun `should round-trip boolean schema`() {
        val schema = Schema(JsonPrimitive(false))
        TestHelpers.testRoundTripWithoutValidation(Schema.serializer(), schema)
    }

    @Test
    fun `should round-trip object schema`() {
        val schemaObject = buildJsonObject {
            put("type", "array")
            put("items", buildJsonObject {
                put("type", "string")
            })
        }
        val schema = Schema(schemaObject)
        TestHelpers.testRoundTripWithoutValidation(Schema.serializer(), schema)
    }

    // Example Tests

    @Test
    fun `should serialize Example with value only`() {
        val example = Example(
            value = JsonPrimitive("example value")
        )
        val json = compactJson.encodeToString(example)

        assertEquals("""{"value":"example value"}""", json)
    }

    @Test
    fun `should serialize Example with summary and description`() {
        val example = Example(
            summary = "Example summary",
            description = "Example description",
            value = buildJsonObject {
                put("name", "John Doe")
                put("age", 30)
            }
        )
        val json = compactJson.encodeToString(example)

        val expected = """{"summary":"Example summary","description":"Example description","value":{"name":"John Doe","age":30}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Example with dataValue`() {
        val example = Example(
            dataValue = buildJsonObject {
                put("id", 123)
                put("status", "active")
            }
        )
        val json = compactJson.encodeToString(example)

        assertEquals("""{"dataValue":{"id":123,"status":"active"}}""", json)
    }

    @Test
    fun `should serialize Example with serializedValue`() {
        val example = Example(
            serializedValue = """{"key":"value"}"""
        )
        val json = compactJson.encodeToString(example)

        assertEquals("""{"serializedValue":"{\"key\":\"value\"}"}""", json)
    }

    @Test
    fun `should serialize Example with externalValue`() {
        val example = Example(
            summary = "External example",
            externalValue = "https://example.com/examples/user.json"
        )
        val json = compactJson.encodeToString(example)

        val expected = """{"summary":"External example","externalValue":"https://example.com/examples/user.json"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Example with extensions`() {
        val example = Example(
            value = JsonPrimitive("test"),
            extensions = mapOf("x-internal-id" to JsonPrimitive("12345"))
        )
        val json = compactJson.encodeToString(example)

        assertEquals("""{"value":"test","extensions":{"x-internal-id":"12345"}}""", json)
    }

    @Test
    fun `should reject Example with both value and externalValue`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Example(
                value = JsonPrimitive("test"),
                externalValue = "https://example.com/test.json"
            )
        }

        assertEquals(
            "Example 'value' and 'externalValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `should reject Example with both value and dataValue`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Example(
                value = JsonPrimitive("test"),
                dataValue = JsonPrimitive("data")
            )
        }

        assertEquals(
            "Example 'value' and 'dataValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `should reject Example with both value and serializedValue`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Example(
                value = JsonPrimitive("test"),
                serializedValue = "serialized"
            )
        }

        assertEquals(
            "Example 'value' and 'serializedValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `should reject Example with both serializedValue and externalValue`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Example(
                serializedValue = "serialized",
                externalValue = "https://example.com/test.json"
            )
        }

        assertEquals(
            "Example 'serializedValue' and 'externalValue' are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `should allow Example with dataValue and serializedValue`() {
        // dataValue and serializedValue can coexist
        val example = Example(
            dataValue = JsonPrimitive("data"),
            serializedValue = "serialized"
        )
        val json = compactJson.encodeToString(example)

        assertEquals("""{"dataValue":"data","serializedValue":"serialized"}""", json)
    }

    @Test
    fun `should allow Example with dataValue and externalValue`() {
        // dataValue and externalValue can coexist
        val example = Example(
            dataValue = JsonPrimitive("data"),
            externalValue = "https://example.com/test.json"
        )
        val json = compactJson.encodeToString(example)

        assertEquals("""{"dataValue":"data","externalValue":"https://example.com/test.json"}""", json)
    }

    @Test
    fun `should deserialize Example with value`() {
        val json = """{"summary":"Test","value":{"foo":"bar"}}"""
        val example = compactJson.decodeFromString<Example>(json)

        assertEquals("Test", example.summary)
        val expectedValue = buildJsonObject {
            put("foo", "bar")
        }
        assertEquals(expectedValue, example.value)
    }

    @Test
    fun `should round-trip Example with value`() {
        val example = Example(
            summary = "User example",
            description = "An example user object",
            value = buildJsonObject {
                put("username", "johndoe")
                put("email", "john@example.com")
            }
        )

        TestHelpers.testRoundTripWithoutValidation(Example.serializer(), example)
    }

    @Test
    fun `should round-trip Example with externalValue`() {
        val example = Example(
            summary = "External reference",
            externalValue = "https://api.example.com/examples/user.json"
        )

        TestHelpers.testRoundTripWithoutValidation(Example.serializer(), example)
    }

    @Test
    fun `should serialize Example with complex nested value`() {
        val example = Example(
            summary = "Complex example",
            value = buildJsonObject {
                put("users", buildJsonArray {
                    add(buildJsonObject {
                        put("id", 1)
                        put("name", "Alice")
                        put("roles", buildJsonArray {
                            add("admin")
                            add("user")
                        })
                    })
                    add(buildJsonObject {
                        put("id", 2)
                        put("name", "Bob")
                        put("roles", buildJsonArray {
                            add("user")
                        })
                    })
                })
            }
        )
        val json = compactJson.encodeToString(example)

        val expected = """{"summary":"Complex example","value":{"users":[{"id":1,"name":"Alice","roles":["admin","user"]},{"id":2,"name":"Bob","roles":["user"]}]}}"""
        assertEquals(expected, json)
    }
}
