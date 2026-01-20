package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.opentest4j.AssertionFailedError

/**
 * Helper functions for testing OpenAPI model classes.
 */
object TestHelpers {
    /**
     * Tests serialization of an object to JSON and validates against OpenAPI schema.
     *
     * @param T the type of the object
     * @param serializer the kotlinx.serialization serializer for type T
     * @param obj the object to serialize
     * @param json the Json instance to use (defaults to openApiJson)
     * @return the serialized JSON string
     */
    fun <T> testSerialization(
        serializer: KSerializer<T>,
        obj: T,
        json: Json = openApiJson,
    ): String {
        val jsonString = json.encodeToString(serializer, obj)
        jsonString.validateOpenAPI()
        return jsonString
    }

    /**
     * Tests deserialization of JSON back to an object.
     *
     * @param T the type of the object
     * @param serializer the kotlinx.serialization serializer for type T
     * @param jsonString the JSON string to deserialize
     * @param json the Json instance to use (defaults to openApiJson)
     * @return the deserialized object
     */
    fun <T> testDeserialization(
        serializer: KSerializer<T>,
        jsonString: String,
        json: Json = openApiJson,
    ): T {
        return json.decodeFromString(serializer, jsonString)
    }

    /**
     * Tests round-trip serialization: object -> JSON -> object.
     *
     * Verifies that serializing an object to JSON and deserializing it back
     * produces an equal object.
     *
     * @param T the type of the object
     * @param serializer the kotlinx.serialization serializer for type T
     * @param obj the object to test
     * @param json the Json instance to use (defaults to openApiJson)
     */
    fun <T> testRoundTrip(
        serializer: KSerializer<T>,
        obj: T,
        json: Json = openApiJson,
    ) {
        val jsonString = testSerialization(serializer, obj, json)
        val deserialized = testDeserialization(serializer, jsonString, json)
        assertEquals(obj, deserialized, "Round-trip serialization should produce equal objects")
    }

    /**
     * Tests round-trip serialization without OpenAPI schema validation.
     *
     * Useful for testing individual objects that are not complete OpenAPI documents.
     *
     * @param T the type of the object
     * @param serializer the kotlinx.serialization serializer for type T
     * @param obj the object to test
     * @param json the Json instance to use (defaults to compactJson)
     */
    fun <T> testRoundTripWithoutValidation(
        serializer: KSerializer<T>,
        obj: T,
        json: Json = compactJson,
    ) {
        val jsonString = json.encodeToString(serializer, obj)
        val deserialized = json.decodeFromString(serializer, jsonString)
        assertEquals(obj, deserialized, "Round-trip serialization should produce equal objects")
    }

    /**
     * Tests that a JSON string can be deserialized and validates against OpenAPI schema.
     *
     * @param T the type of the object
     * @param serializer the kotlinx.serialization serializer for type T
     * @param jsonString the JSON string to test
     * @param json the Json instance to use (defaults to openApiJson)
     * @return the deserialized object
     */
    fun <T> testJsonDeserialization(
        serializer: KSerializer<T>,
        jsonString: String,
        json: Json = openApiJson,
    ): T {
        jsonString.validateOpenAPI()
        return testDeserialization(serializer, jsonString, json)
    }
}

/**
 * Loads a resource file as a string.
 *
 * @param resourcePath the path to the resource file
 * @return the file contents as a string
 */
fun loadResource(resourcePath: String): String {
    return object {}.javaClass.classLoader
        .getResourceAsStream(resourcePath)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Could not load resource: $resourcePath")
}

/**
 * Compares two JSON strings by their content (parsed structure), not by string representation.
 *
 * This allows using formatted JSON in tests with proper indentation while still
 * comparing the actual JSON structure. Supports IntelliJ's `//language=JSON` annotation
 * for syntax highlighting.
 *
 * Example usage:
 * ```kotlin
 * assertJsonEquals(
 *     //language=JSON
 *     """
 *     {
 *       "type": "object",
 *       "properties": {
 *         "name": { "type": "string" }
 *       }
 *     }
 *     """,
 *     actualJson
 * )
 * ```
 *
 * @param expected the expected JSON string (can be formatted with whitespace)
 * @param actual the actual JSON string to compare
 * @param message optional message to display on failure
 */
fun assertJsonEquals(expected: String, actual: String, message: String? = null) {
    val expectedElement: JsonElement = try {
        compactJson.parseToJsonElement(expected)
    } catch (e: Exception) {
        throw AssertionFailedError(
            "Failed to parse expected JSON: ${e.message}",
            expected,
            actual
        )
    }

    val actualElement: JsonElement = try {
        compactJson.parseToJsonElement(actual)
    } catch (e: Exception) {
        throw AssertionFailedError(
            "Failed to parse actual JSON: ${e.message}",
            expected,
            actual
        )
    }

    if (expectedElement != actualElement) {
        val prettyExpected = openApiJson.encodeToString(JsonElement.serializer(), expectedElement)
        val prettyActual = openApiJson.encodeToString(JsonElement.serializer(), actualElement)
        throw AssertionFailedError(
            message ?: "JSON content mismatch",
            prettyExpected,
            prettyActual
        )
    }
}

/**
 * Compares a serialized object's JSON with expected JSON by content.
 *
 * Convenience overload that serializes the actual object before comparison.
 *
 * Example usage:
 * ```kotlin
 * val schema = schemaOf<User>()
 * assertJsonEquals(
 *     //language=JSON
 *     """
 *     {
 *       "type": "object",
 *       "required": ["name"],
 *       "properties": {
 *         "name": { "type": "string" }
 *       }
 *     }
 *     """,
 *     schema
 * )
 * ```
 *
 * @param expected the expected JSON string (can be formatted with whitespace)
 * @param actual the object to serialize and compare
 * @param message optional message to display on failure
 */
inline fun <reified T> assertJsonEquals(expected: String, actual: T, message: String? = null) {
    val actualJson = compactJson.encodeToString(actual)
    assertJsonEquals(expected, actualJson, message)
}
