package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals

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
