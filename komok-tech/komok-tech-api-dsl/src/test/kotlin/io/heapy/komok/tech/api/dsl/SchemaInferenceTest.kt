package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SchemaInferenceTest {

    // ============================================
    // Basic Type Inference Tests
    // ============================================

    @Test
    fun `should infer schema from simple data class`() {
        data class User(val name: String, val age: Int)

        val schema = schemaOf<User>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["name", "age"],
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should infer string type`() {
        data class TestClass(val value: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "string" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should infer integer types`() {
        data class TestClass(
            val intVal: Int,
            val longVal: Long,
            val shortVal: Short,
            val byteVal: Byte
        )

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["intVal", "longVal", "shortVal", "byteVal"],
              "properties": {
                "intVal": { "type": "integer" },
                "longVal": { "type": "integer" },
                "shortVal": { "type": "integer" },
                "byteVal": { "type": "integer" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should infer number types`() {
        data class TestClass(val floatVal: Float, val doubleVal: Double)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["floatVal", "doubleVal"],
              "properties": {
                "floatVal": { "type": "number" },
                "doubleVal": { "type": "number" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should infer boolean type`() {
        data class TestClass(val active: Boolean)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["active"],
              "properties": {
                "active": { "type": "boolean" }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Nullable Type Tests
    // ============================================

    @Test
    fun `should handle nullable types with OpenAPI 3_0 style`() {
        data class TestClass(val optional: String?)

        val schema = schemaOf<TestClass>(SchemaInferenceConfig(openApi30Nullable = true))

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "properties": {
                "optional": { "type": "string", "nullable": true }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should handle nullable types with JSON Schema style`() {
        data class TestClass(val optional: String?)

        val schema = schemaOf<TestClass>(SchemaInferenceConfig(openApi30Nullable = false))

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "properties": {
                "optional": {
                  "anyOf": [
                    { "type": "string" },
                    { "type": "null" }
                  ]
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should mark non-nullable properties as required`() {
        data class TestClass(val required: String, val optional: String?)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["required"],
              "properties": {
                "required": { "type": "string" },
                "optional": { "type": "string", "nullable": true }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should not mark properties with defaults as required`() {
        data class TestClass(val withDefault: String = "default", val noDefault: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["noDefault"],
              "properties": {
                "withDefault": { "type": "string" },
                "noDefault": { "type": "string" }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Collection Tests
    // ============================================

    @Test
    fun `should handle List as array`() {
        data class TestClass(val items: List<String>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": { "type": "string" }
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should handle Set as array`() {
        data class TestClass(val items: Set<Int>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": { "type": "integer" }
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should handle nested List of objects`() {
        data class Item(val name: String)
        data class TestClass(val items: List<Item>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "required": ["name"],
                    "properties": {
                      "name": { "type": "string" }
                    }
                  }
                }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Map Tests
    // ============================================

    @Test
    fun `should handle Map with additionalProperties`() {
        data class TestClass(val metadata: Map<String, Int>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["metadata"],
              "properties": {
                "metadata": {
                  "type": "object",
                  "additionalProperties": { "type": "integer" }
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `should handle Map with object values`() {
        data class Value(val data: String)
        data class TestClass(val values: Map<String, Value>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["values"],
              "properties": {
                "values": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "object",
                    "required": ["data"],
                    "properties": {
                      "data": { "type": "string" }
                    }
                  }
                }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Enum Tests
    // ============================================

    @Test
    fun `should handle enums`() {
        val schema = schemaOf<TestStatus>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "string",
              "enum": ["ACTIVE", "INACTIVE", "PENDING"]
            }
            """,
            schema
        )
    }

    @Test
    fun `should handle enum property`() {
        data class TestClass(val status: TestStatus)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["status"],
              "properties": {
                "status": {
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"]
                }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Sealed Class Tests
    // ============================================

    @Test
    fun `should handle sealed classes with oneOf`() {
        val schema = schemaOf<TestShape>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "oneOf": [
                {
                  "type": "object",
                  "required": ["radius"],
                  "properties": {
                    "radius": { "type": "number" }
                  }
                },
                {
                  "type": "object",
                  "required": ["width", "height"],
                  "properties": {
                    "width": { "type": "number" },
                    "height": { "type": "number" }
                  }
                }
              ]
            }
            """,
            schema
        )
    }

    // ============================================
    // Nested Data Class Tests
    // ============================================

    @Test
    fun `should handle nested data classes`() {
        data class Address(val city: String, val zip: String)
        data class User(val name: String, val address: Address)

        val schema = schemaOf<User>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["name", "address"],
              "properties": {
                "name": { "type": "string" },
                "address": {
                  "type": "object",
                  "required": ["city", "zip"],
                  "properties": {
                    "city": { "type": "string" },
                    "zip": { "type": "string" }
                  }
                }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Recursion Tests
    // ============================================

    @Test
    fun `should prevent infinite recursion with self-referential types`() {
        // Should not throw StackOverflowError
        val schema = schemaOf<TestNode>()

        // Verify schema was generated
        assertNotNull(schema)

        // The recursive reference should produce a placeholder
        assertJsonEquals(
            //language=JSON
            $$"""
            {
              "type": "object",
              "required": ["value", "children"],
              "properties": {
                "value": { "type": "string" },
                "children": {
                  "type": "array",
                  "items": {
                    "$comment": "Recursive reference to TestNode",
                    "type": "object"
                  }
                }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // SerialName Tests
    // ============================================

    @Test
    fun `should respect SerialName annotation on properties`() {
        data class TestClass(
            @SerialName("user_name")
            val userName: String
        )

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["user_name"],
              "properties": {
                "user_name": { "type": "string" }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Annotation Tests - Numeric Constraints
    // ============================================

    @Test
    fun `Min annotation should set minimum value`() {
        data class TestClass(@Min(5) val value: Int)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "integer", "minimum": 5 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `Max annotation should set maximum value`() {
        data class TestClass(@Max(100) val value: Int)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "integer", "maximum": 100 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `Positive annotation should set exclusiveMinimum to 0`() {
        data class TestClass(@Positive val value: Int)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "integer", "exclusiveMinimum": 0 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `Negative annotation should set exclusiveMaximum to 0`() {
        data class TestClass(@Negative val value: Int)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "integer", "exclusiveMaximum": 0 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `PositiveOrZero annotation should set minimum to 0`() {
        data class TestClass(@PositiveOrZero val value: Int)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "integer", "minimum": 0 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `NegativeOrZero annotation should set maximum to 0`() {
        data class TestClass(@NegativeOrZero val value: Int)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "integer", "maximum": 0 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `MultipleOf annotation should set multipleOf`() {
        data class TestClass(@MultipleOf(0.5) val value: Double)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "number", "multipleOf": 0.5 }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Annotation Tests - String Constraints
    // ============================================

    @Test
    fun `SchemaMinLength annotation should set minLength`() {
        data class TestClass(@SchemaMinLength(5) val value: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "string", "minLength": 5 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaMaxLength annotation should set maxLength`() {
        data class TestClass(@SchemaMaxLength(100) val value: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "string", "maxLength": 100 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `NotEmpty annotation should set minLength to 1`() {
        data class TestClass(@NotEmpty val value: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "string", "minLength": 1 }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `NotBlank annotation should set minLength and pattern`() {
        data class TestClass(@NotBlank val value: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "string", "minLength": 1, "pattern": ".*\\S.*" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaPattern annotation should set pattern`() {
        data class TestClass(@SchemaPattern("^[a-z]+$") val value: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["value"],
              "properties": {
                "value": { "type": "string", "pattern": "^[a-z]+$" }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Annotation Tests - Format Hints
    // ============================================

    @Test
    fun `Email annotation should set email format`() {
        data class TestClass(@Email val email: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["email"],
              "properties": {
                "email": { "type": "string", "format": "email" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `Url annotation should set uri format`() {
        data class TestClass(@Url val url: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["url"],
              "properties": {
                "url": { "type": "string", "format": "uri" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `Uuid annotation should set uuid format`() {
        data class TestClass(@Uuid val id: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["id"],
              "properties": {
                "id": { "type": "string", "format": "uuid" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `Date annotation should set date format`() {
        data class TestClass(@Date val date: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["date"],
              "properties": {
                "date": { "type": "string", "format": "date" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `DateTime annotation should set date-time format`() {
        data class TestClass(@DateTime val timestamp: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["timestamp"],
              "properties": {
                "timestamp": { "type": "string", "format": "date-time" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaFormat annotation should set custom format`() {
        data class TestClass(@SchemaFormat("int64") val bigNumber: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["bigNumber"],
              "properties": {
                "bigNumber": { "type": "string", "format": "int64" }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Annotation Tests - Array Constraints
    // ============================================

    @Test
    fun `SchemaMinItems annotation should set minItems`() {
        data class TestClass(@SchemaMinItems(1) val items: List<String>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": { "type": "string" },
                  "minItems": 1
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaMaxItems annotation should set maxItems`() {
        data class TestClass(@SchemaMaxItems(10) val items: List<String>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": { "type": "string" },
                  "maxItems": 10
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `UniqueItems annotation should set uniqueItems to true`() {
        data class TestClass(@UniqueItems val items: List<String>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": { "type": "string" },
                  "uniqueItems": true
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `NotEmpty on array should set minItems to 1`() {
        data class TestClass(@NotEmpty val items: List<String>)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["items"],
              "properties": {
                "items": {
                  "type": "array",
                  "items": { "type": "string" },
                  "minItems": 1
                }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Annotation Tests - Documentation
    // ============================================

    @Test
    fun `SchemaTitle annotation should set title on property`() {
        data class TestClass(@SchemaTitle("User Name") val name: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string", "title": "User Name" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaDescription annotation should set description on property`() {
        data class TestClass(@SchemaDescription("The user's full name") val name: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string", "description": "The user's full name" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaTitle annotation should set title on class`() {
        @SchemaTitle("User Object")
        data class TestClass(val name: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "title": "User Object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `SchemaDescription annotation should set description on class`() {
        @SchemaDescription("Represents a user in the system")
        data class TestClass(val name: String)

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "description": "Represents a user in the system",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """,
            schema
        )
    }

    // ============================================
    // Combined Annotations Tests
    // ============================================

    @Test
    fun `should support multiple annotations on same property`() {
        data class TestClass(
            @SchemaMinLength(1)
            @SchemaMaxLength(100)
            @SchemaDescription("User name")
            val name: String
        )

        val schema = schemaOf<TestClass>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": {
                  "type": "string",
                  "minLength": 1,
                  "maxLength": 100,
                  "description": "User name"
                }
              }
            }
            """,
            schema
        )
    }

    @Test
    fun `comprehensive example with all features`() {
        val schema = schemaOf<TestUser>()

        assertJsonEquals(
            //language=JSON
            """
            {
              "type": "object",
              "title": "User",
              "description": "A user in the system",
              "required": ["name", "email", "age", "roles"],
              "properties": {
                "name": {
                  "type": "string",
                  "minLength": 1,
                  "maxLength": 50
                },
                "email": {
                  "type": "string",
                  "format": "email"
                },
                "age": {
                  "type": "integer",
                  "minimum": 0,
                  "maximum": 150
                },
                "roles": {
                  "type": "array",
                  "items": { "type": "string" },
                  "minItems": 1
                },
                "address": {
                  "type": "object",
                  "required": ["city", "zip"],
                  "properties": {
                    "city": {
                      "type": "string",
                      "minLength": 1,
                      "pattern": ".*\\S.*"
                    },
                    "zip": {
                      "type": "string",
                      "pattern": "^\\d{5}$"
                    }
                  },
                  "nullable": true
                },
                "status": {
                  "type": "string",
                  "enum": ["ACTIVE", "INACTIVE", "PENDING"]
                }
              }
            }
            """,
            schema
        )
    }
}

// Test helper types defined at file level for reflection to work
enum class TestStatus { ACTIVE, INACTIVE, PENDING }

sealed interface TestShape
data class TestCircle(val radius: Double) : TestShape
data class TestRectangle(val width: Double, val height: Double) : TestShape

data class TestNode(val value: String, val children: List<TestNode>)

data class TestAddress(
    @NotBlank val city: String,
    @SchemaPattern("^\\d{5}$") val zip: String
)

@SchemaTitle("User")
@SchemaDescription("A user in the system")
data class TestUser(
    @SchemaMinLength(1)
    @SchemaMaxLength(50)
    val name: String,

    @Email
    val email: String,

    @Min(0)
    @Max(150)
    val age: Int,

    @NotEmpty
    val roles: List<String>,

    val address: TestAddress?,

    val status: TestStatus = TestStatus.ACTIVE
)
