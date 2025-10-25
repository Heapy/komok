package io.heapy.komok.tech.api.dsl

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenAPISchemaValidatorTest {
    @Test
    fun `should load OpenAPI schema successfully`() {
        // When loading the OpenAPI schema
        val schema = OpenAPISchemaValidator.openApiSchema

        // Then
        assertNotNull(schema)
    }

    @Test
    fun `should validate minimal valid OpenAPI document`() {
        // Given a minimal valid OpenAPI 3.2 document
        val validJson = """
            {
              "openapi": "3.2.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {}
            }
        """.trimIndent()

        // When validating
        val errors = OpenAPISchemaValidator.validate(validJson)

        // Then
        assertTrue(errors.isEmpty(), "Valid document should have no errors, but got: $errors")
    }

    @Test
    fun `should reject invalid OpenAPI document`() {
        // Given an invalid OpenAPI document (missing required fields)
        val invalidJson = """
            {
              "openapi": "3.2.0"
            }
        """.trimIndent()

        // When validating
        val errors = OpenAPISchemaValidator.validate(invalidJson)

        // Then
        assertTrue(errors.isNotEmpty(), "Invalid document should have errors")
    }

    @Test
    fun `validateAndAssert should throw for invalid document`() {
        // Given an invalid OpenAPI document
        val invalidJson = """
            {
              "openapi": "3.2.0"
            }
        """.trimIndent()

        // When validating with assert
        val exception = assertThrows(IllegalStateException::class.java) {
            OpenAPISchemaValidator.validateAndAssert(invalidJson)
        }

        // Then
        assertTrue(exception.message!!.contains("validation failed"))
    }

    @Test
    fun `extension function should validate valid JSON`() {
        // Given a valid OpenAPI document
        val validJson = """
            {
              "openapi": "3.2.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {}
            }
        """.trimIndent()

        // When using extension function
        validJson.validateOpenAPI()

        // Then no exception is thrown
    }

    @Test
    fun `should validate document with components instead of paths`() {
        // Given a valid OpenAPI document with components but no paths
        val validJson = """
            {
              "openapi": "3.2.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "components": {
                "schemas": {
                  "Pet": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        // When validating
        validJson.validateOpenAPI()

        // Then no exception is thrown
    }
}
