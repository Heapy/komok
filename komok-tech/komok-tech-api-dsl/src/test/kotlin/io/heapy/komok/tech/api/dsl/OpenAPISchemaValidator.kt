package io.heapy.komok.tech.api.dsl

import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.InputStream

/**
 * Helper class for validating JSON against OpenAPI 3.2 JSON Schema.
 */
object OpenAPISchemaValidator {
    private val jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)

    /**
     * Lazily loaded OpenAPI 3.2 JSON Schema.
     */
    val openApiSchema: JsonSchema by lazy {
        val schemaStream: InputStream = javaClass.classLoader
            .getResourceAsStream("openapi-v3.2.0-json-schema.json")
            ?: error("Could not load openapi-v3.2.0-json-schema.json from resources")

        jsonSchemaFactory.getSchema(schemaStream)
    }

    /**
     * Validates a JSON string against the OpenAPI 3.2 schema.
     *
     * @param json the JSON string to validate
     * @return set of validation errors, empty if valid
     */
    fun validate(json: String): Set<ValidationMessage> {
        val jsonNode = com.fasterxml.jackson.databind.ObjectMapper().readTree(json)
        return openApiSchema.validate(jsonNode)
    }

    /**
     * Validates a JSON string and throws an exception if invalid.
     *
     * @param json the JSON string to validate
     * @throws AssertionError if validation fails
     */
    fun validateAndAssert(json: String) {
        val errors = validate(json)
        if (errors.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("OpenAPI schema validation failed with ${errors.size} error(s):")
                errors.forEachIndexed { index, error ->
                    appendLine("${index + 1}. ${error.message}")
                    appendLine("   Type: ${error.type}")
                }
            }
            error(errorMessage)
        }
    }
}

/**
 * JSON instance configured for OpenAPI serialization.
 */
val openApiJson = Json {
    prettyPrint = true
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

/**
 * JSON instance for compact serialization (no pretty print).
 */
val compactJson = Json {
    prettyPrint = false
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

/**
 * Extension function to validate a JsonElement against OpenAPI schema.
 */
fun JsonElement.validateOpenAPI() {
    OpenAPISchemaValidator.validateAndAssert(this.toString())
}

/**
 * Extension function to validate a JSON string against OpenAPI schema.
 */
fun String.validateOpenAPI() {
    OpenAPISchemaValidator.validateAndAssert(this)
}
