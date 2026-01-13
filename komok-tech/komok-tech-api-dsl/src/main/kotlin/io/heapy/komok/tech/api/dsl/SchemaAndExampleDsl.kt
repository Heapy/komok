package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * DSL builder for [Schema] object using JSON Schema Draft 2020-12.
 *
 * Provides helper methods for building common JSON Schema patterns.
 *
 * Example usage:
 * ```kotlin
 * val schema = schema {
 *     type = "object"
 *     required("name", "email")
 *     properties {
 *         "name" to stringSchema { minLength = 1 }
 *         "email" to stringSchema { format = "email" }
 *         "age" to integerSchema { minimum = 0 }
 *     }
 * }
 * ```
 */
class SchemaBuilder {
    private val properties = mutableMapOf<String, JsonElement>()

    var type: String? = null
    var format: String? = null
    var title: String? = null
    var description: String? = null
    var default: JsonElement? = null
    var enum: List<JsonElement>? = null
    var const: JsonElement? = null

    // String constraints
    var minLength: Int? = null
    var maxLength: Int? = null
    var pattern: String? = null

    // Numeric constraints
    var minimum: Number? = null
    var maximum: Number? = null
    var exclusiveMinimum: Number? = null
    var exclusiveMaximum: Number? = null
    var multipleOf: Number? = null

    // Array constraints
    var items: Schema? = null
    var minItems: Int? = null
    var maxItems: Int? = null
    var uniqueItems: Boolean? = null

    // Object constraints
    var minProperties: Int? = null
    var maxProperties: Int? = null
    var additionalProperties: JsonElement? = null

    // Reference
    var ref: String? = null

    // Composition
    var allOf: List<Schema>? = null
    var anyOf: List<Schema>? = null
    var oneOf: List<Schema>? = null
    var not: Schema? = null

    // Nullable
    var nullable: Boolean? = null

    // Required fields for object schemas
    private var requiredFields: List<String>? = null

    // Object properties
    @PublishedApi
    internal var schemaProperties: Map<String, Schema>? = null

    /**
     * Sets the required fields for an object schema.
     */
    fun required(vararg fields: String) {
        requiredFields = fields.toList()
    }

    /**
     * Sets the enum values from varargs of strings.
     */
    fun enum(vararg values: String) {
        enum = values.map { JsonPrimitive(it) }
    }

    /**
     * Configures object properties using DSL syntax.
     */
    inline fun properties(block: SchemaPropertiesBuilder.() -> Unit) {
        schemaProperties = schemaProperties(block)
    }

    /**
     * Sets a custom property on the schema.
     */
    fun property(name: String, value: JsonElement) {
        properties[name] = value
    }

    fun build(): Schema {
        // If ref is set, return a reference schema
        if (ref != null) {
            return Schema(buildJsonObject {
                put("\$ref", ref)
            })
        }

        return Schema(buildJsonObject {
            type?.let { put("type", it) }
            format?.let { put("format", it) }
            title?.let { put("title", it) }
            description?.let { put("description", it) }
            default?.let { put("default", it) }
            enum?.let { put("enum", JsonArray(it)) }
            const?.let { put("const", it) }

            // String constraints
            minLength?.let { put("minLength", it) }
            maxLength?.let { put("maxLength", it) }
            pattern?.let { put("pattern", it) }

            // Numeric constraints
            minimum?.let { put("minimum", it) }
            maximum?.let { put("maximum", it) }
            exclusiveMinimum?.let { put("exclusiveMinimum", it) }
            exclusiveMaximum?.let { put("exclusiveMaximum", it) }
            multipleOf?.let { put("multipleOf", it) }

            // Array constraints
            items?.let { put("items", it.schema) }
            minItems?.let { put("minItems", it) }
            maxItems?.let { put("maxItems", it) }
            uniqueItems?.let { put("uniqueItems", it) }

            // Object constraints
            requiredFields?.let { put("required", JsonArray(it.map { f -> JsonPrimitive(f) })) }
            schemaProperties?.let {
                put("properties", buildJsonObject {
                    it.forEach { (name, schema) -> put(name, schema.schema) }
                })
            }
            minProperties?.let { put("minProperties", it) }
            maxProperties?.let { put("maxProperties", it) }
            additionalProperties?.let { put("additionalProperties", it) }

            // Composition
            allOf?.let { put("allOf", JsonArray(it.map { s -> s.schema })) }
            anyOf?.let { put("anyOf", JsonArray(it.map { s -> s.schema })) }
            oneOf?.let { put("oneOf", JsonArray(it.map { s -> s.schema })) }
            not?.let { put("not", it.schema) }

            // Nullable (OpenAPI 3.0 style)
            nullable?.let { put("nullable", it) }

            // Custom properties
            properties.forEach { (name, value) -> put(name, value) }
        })
    }
}

/**
 * DSL builder for schema properties map.
 */
class SchemaPropertiesBuilder {
    @PublishedApi
    internal val properties = mutableMapOf<String, Schema>()

    /**
     * Adds a property schema using DSL syntax.
     */
    inline infix fun String.to(block: SchemaBuilder.() -> Unit) {
        properties[this] = schema(block)
    }

    /**
     * Adds a pre-built schema as a property.
     */
    infix fun String.to(schema: Schema) {
        properties[this] = schema
    }

    fun build(): Map<String, Schema> = properties.toMap()
}

/**
 * Creates a map of schema properties using DSL syntax.
 */
inline fun schemaProperties(block: SchemaPropertiesBuilder.() -> Unit): Map<String, Schema> {
    return SchemaPropertiesBuilder().apply(block).build()
}

/**
 * Creates a [Schema] object using DSL syntax.
 *
 * @param block configuration block for the schema
 * @return configured Schema object
 */
inline fun schema(block: SchemaBuilder.() -> Unit): Schema {
    return SchemaBuilder().apply(block).build()
}

/**
 * Creates a Schema from a raw JsonElement.
 */
fun schema(json: JsonElement): Schema = Schema(json)

/**
 * Creates a boolean schema (true = any valid, false = none valid).
 */
fun booleanSchema(value: Boolean): Schema = Schema(JsonPrimitive(value))

/**
 * Creates a reference schema.
 */
fun refSchema(ref: String): Schema = Schema(buildJsonObject {
    put("\$ref", ref)
})

/**
 * Creates a string type schema.
 */
inline fun stringSchema(block: SchemaBuilder.() -> Unit = {}): Schema {
    return SchemaBuilder().apply {
        type = "string"
        block()
    }.build()
}

/**
 * Creates an integer type schema.
 */
inline fun integerSchema(block: SchemaBuilder.() -> Unit = {}): Schema {
    return SchemaBuilder().apply {
        type = "integer"
        block()
    }.build()
}

/**
 * Creates a number type schema.
 */
inline fun numberSchema(block: SchemaBuilder.() -> Unit = {}): Schema {
    return SchemaBuilder().apply {
        type = "number"
        block()
    }.build()
}

/**
 * Creates a boolean type schema.
 */
inline fun booleanTypeSchema(block: SchemaBuilder.() -> Unit = {}): Schema {
    return SchemaBuilder().apply {
        type = "boolean"
        block()
    }.build()
}

/**
 * Creates an array type schema.
 */
inline fun arraySchema(block: SchemaBuilder.() -> Unit = {}): Schema {
    return SchemaBuilder().apply {
        type = "array"
        block()
    }.build()
}

/**
 * Creates an object type schema.
 */
inline fun objectSchema(block: SchemaBuilder.() -> Unit = {}): Schema {
    return SchemaBuilder().apply {
        type = "object"
        block()
    }.build()
}

// ============================================
// Example DSL
// ============================================

/**
 * DSL builder for [Example] object.
 *
 * The builder enforces fail-fast validation for mutual exclusivity:
 * - `value` and `externalValue` are mutually exclusive
 * - `value` and `dataValue` are mutually exclusive
 * - `value` and `serializedValue` are mutually exclusive
 * - `serializedValue` and `externalValue` are mutually exclusive
 *
 * Example usage:
 * ```kotlin
 * val example = example {
 *     summary = "User example"
 *     description = "An example user object"
 *     value = buildJsonObject {
 *         put("name", "John Doe")
 *         put("email", "john@example.com")
 *     }
 * }
 * ```
 */
class ExampleBuilder {
    var summary: String? = null
    var description: String? = null
    var value: JsonElement? = null
    var dataValue: JsonElement? = null
    var serializedValue: String? = null
    var externalValue: String? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Sets the value using a JSON object builder.
     */
    inline fun value(block: JsonObjectBuilderScope.() -> Unit) {
        value = buildJsonObject {
            JsonObjectBuilderScope(this).block()
        }
    }

    /**
     * Sets the dataValue using a JSON object builder.
     */
    inline fun dataValue(block: JsonObjectBuilderScope.() -> Unit) {
        dataValue = buildJsonObject {
            JsonObjectBuilderScope(this).block()
        }
    }

    fun build(): Example {
        // Validate mutual exclusivity
        require(!(value != null && externalValue != null)) {
            "Example 'value' and 'externalValue' are mutually exclusive. Only one should be specified."
        }
        require(!(value != null && dataValue != null)) {
            "Example 'value' and 'dataValue' are mutually exclusive. Only one should be specified."
        }
        require(!(value != null && serializedValue != null)) {
            "Example 'value' and 'serializedValue' are mutually exclusive. Only one should be specified."
        }
        require(!(serializedValue != null && externalValue != null)) {
            "Example 'serializedValue' and 'externalValue' are mutually exclusive. Only one should be specified."
        }

        return Example(
            summary = summary,
            description = description,
            value = value,
            dataValue = dataValue,
            serializedValue = serializedValue,
            externalValue = externalValue,
            extensions = extensions,
        )
    }
}

/**
 * Wrapper class for JsonObjectBuilder to allow DSL syntax.
 */
class JsonObjectBuilderScope(private val builder: kotlinx.serialization.json.JsonObjectBuilder) {
    fun put(key: String, value: String) = builder.put(key, value)
    fun put(key: String, value: Number) = builder.put(key, value)
    fun put(key: String, value: Boolean) = builder.put(key, value)
    fun put(key: String, value: JsonElement) = builder.put(key, value)
    fun putJsonObject(key: String, block: JsonObjectBuilderScope.() -> Unit) {
        builder.put(key, buildJsonObject {
            JsonObjectBuilderScope(this).block()
        })
    }
    fun putJsonArray(key: String, block: JsonArrayBuilderScope.() -> Unit) {
        builder.put(key, buildJsonArray {
            JsonArrayBuilderScope(this).block()
        })
    }
}

/**
 * Wrapper class for JsonArrayBuilder to allow DSL syntax.
 */
class JsonArrayBuilderScope(private val builder: kotlinx.serialization.json.JsonArrayBuilder) {
    fun add(value: String) = builder.add(value)
    fun add(value: Number) = builder.add(value)
    fun add(value: Boolean) = builder.add(value)
    fun add(value: JsonElement) = builder.add(value)
    fun addJsonObject(block: JsonObjectBuilderScope.() -> Unit) {
        builder.add(buildJsonObject {
            JsonObjectBuilderScope(this).block()
        })
    }
    fun addJsonArray(block: JsonArrayBuilderScope.() -> Unit) {
        builder.add(buildJsonArray {
            JsonArrayBuilderScope(this).block()
        })
    }
}

/**
 * Creates an [Example] object using DSL syntax.
 *
 * @param block configuration block for the example
 * @return configured Example object
 * @throws IllegalArgumentException if mutually exclusive fields are both set
 */
inline fun example(block: ExampleBuilder.() -> Unit): Example {
    return ExampleBuilder().apply(block).build()
}

/**
 * DSL builder for creating a map of named examples.
 *
 * Example usage:
 * ```kotlin
 * val examples = examples {
 *     "user" to {
 *         summary = "User example"
 *         value = JsonPrimitive("john")
 *     }
 *     "admin" to {
 *         summary = "Admin example"
 *         value = JsonPrimitive("admin")
 *     }
 * }
 * ```
 */
class ExamplesBuilder {
    @PublishedApi
    internal val examples = mutableMapOf<String, Example>()

    /**
     * Adds an example using DSL syntax.
     */
    inline infix fun String.to(block: ExampleBuilder.() -> Unit) {
        examples[this] = example(block)
    }

    /**
     * Adds a pre-built example.
     */
    infix fun String.to(example: Example) {
        examples[this] = example
    }

    fun build(): Map<String, Example> = examples.toMap()
}

/**
 * Creates a map of named examples using DSL syntax.
 */
inline fun examples(block: ExamplesBuilder.() -> Unit): Map<String, Example> {
    return ExamplesBuilder().apply(block).build()
}
