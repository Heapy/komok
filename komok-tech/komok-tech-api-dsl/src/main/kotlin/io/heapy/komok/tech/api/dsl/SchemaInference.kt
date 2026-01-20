package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

/**
 * Configuration options for schema inference.
 *
 * @property openApi30Nullable When true, uses OpenAPI 3.0 style `nullable: true`.
 *                             When false, uses JSON Schema `anyOf` with null type.
 * @property includeDefaults Whether to include default value annotations in schema.
 */
data class SchemaInferenceConfig(
    val openApi30Nullable: Boolean = true,
    val includeDefaults: Boolean = true,
)

/**
 * Infers JSON Schema from Kotlin types at runtime using reflection.
 *
 * Example usage:
 * ```kotlin
 * data class User(
 *     @SchemaMinLength(1)
 *     val name: String,
 *     @Email
 *     val email: String,
 *     @Min(0) @Max(150)
 *     val age: Int
 * )
 *
 * val schema = schemaOf<User>()
 * ```
 *
 * @property config Configuration options for inference behavior
 */
class SchemaInference(
    private val config: SchemaInferenceConfig = SchemaInferenceConfig()
) {
    // Track visited types to prevent infinite recursion
    private val visitedTypes = mutableSetOf<KClass<*>>()

    /**
     * Infers schema from a KType.
     */
    fun infer(type: KType): Schema {
        visitedTypes.clear()
        return Schema(inferInternal(type, emptyList()))
    }

    /**
     * Infers schema from a KClass.
     */
    fun infer(klass: KClass<*>): Schema {
        visitedTypes.clear()
        return Schema(inferFromClass(klass))
    }

    private fun inferInternal(
        type: KType,
        annotations: List<Annotation>
    ): JsonElement {
        val klass = type.jvmErasure
        val isNullable = type.isMarkedNullable

        val baseSchema = when {
            // Primitive types
            klass == String::class -> inferStringSchema(annotations)
            klass == Int::class || klass == Long::class ||
            klass == Short::class || klass == Byte::class -> inferIntegerSchema(annotations)
            klass == Float::class || klass == Double::class -> inferNumberSchema(annotations)
            klass == Boolean::class -> inferBooleanSchema(annotations)

            // Collections
            klass.isSubclassOf(List::class) || klass.isSubclassOf(Set::class) ||
            klass.isSubclassOf(Collection::class) -> {
                val itemType = type.arguments.firstOrNull()?.type
                inferArraySchema(itemType, annotations)
            }

            // Maps
            klass.isSubclassOf(Map::class) -> {
                val valueType = type.arguments.getOrNull(1)?.type
                inferMapSchema(valueType, annotations)
            }

            // Enums
            klass.java.isEnum -> inferEnumSchema(klass, annotations)

            // Sealed classes/interfaces
            klass.isSealed -> inferSealedSchema(klass, annotations)

            // Data classes and regular classes
            klass.isData || klass.primaryConstructor != null -> inferDataClassSchema(klass, annotations)

            // Fallback for unknown types
            else -> buildJsonObject {
                put("type", "object")
            }
        }

        // Handle nullable types
        return if (isNullable) {
            wrapNullable(baseSchema)
        } else {
            baseSchema
        }
    }

    private fun inferStringSchema(annotations: List<Annotation>): JsonElement {
        return buildJsonObject {
            put("type", "string")

            // Process annotations
            annotations.forEach { annotation ->
                when (annotation) {
                    is SchemaMinLength -> put("minLength", annotation.value)
                    is SchemaMaxLength -> put("maxLength", annotation.value)
                    is NotEmpty -> put("minLength", 1)
                    is NotBlank -> {
                        put("minLength", 1)
                        put("pattern", ".*\\S.*")
                    }
                    is SchemaPattern -> put("pattern", annotation.regex)
                    is Email -> put("format", "email")
                    is Url -> put("format", "uri")
                    is Uuid -> put("format", "uuid")
                    is Date -> put("format", "date")
                    is DateTime -> put("format", "date-time")
                    is SchemaFormat -> put("format", annotation.value)
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                    is DefaultString -> if (config.includeDefaults) put("default", annotation.value)
                }
            }
        }
    }

    private fun inferIntegerSchema(annotations: List<Annotation>): JsonElement {
        return buildJsonObject {
            put("type", "integer")
            applyNumericConstraints(annotations)
        }
    }

    private fun inferNumberSchema(annotations: List<Annotation>): JsonElement {
        return buildJsonObject {
            put("type", "number")
            applyNumericConstraints(annotations)
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.applyNumericConstraints(
        annotations: List<Annotation>
    ) {
        annotations.forEach { annotation ->
            when (annotation) {
                is Min -> put("minimum", annotation.value)
                is Max -> put("maximum", annotation.value)
                is ExclusiveMin -> put("exclusiveMinimum", annotation.value)
                is ExclusiveMax -> put("exclusiveMaximum", annotation.value)
                is Positive -> put("exclusiveMinimum", 0)
                is Negative -> put("exclusiveMaximum", 0)
                is PositiveOrZero -> put("minimum", 0)
                is NegativeOrZero -> put("maximum", 0)
                is MultipleOf -> put("multipleOf", annotation.value)
                is SchemaTitle -> put("title", annotation.value)
                is SchemaDescription -> put("description", annotation.value)
                is DefaultNumber -> if (config.includeDefaults) put("default", annotation.value)
            }
        }
    }

    private fun inferBooleanSchema(annotations: List<Annotation>): JsonElement {
        return buildJsonObject {
            put("type", "boolean")
            annotations.forEach { annotation ->
                when (annotation) {
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                    is DefaultBoolean -> if (config.includeDefaults) put("default", annotation.value)
                }
            }
        }
    }

    private fun inferArraySchema(
        itemType: KType?,
        annotations: List<Annotation>
    ): JsonElement {
        return buildJsonObject {
            put("type", "array")

            itemType?.let {
                put("items", inferInternal(it, emptyList()))
            }

            annotations.forEach { annotation ->
                when (annotation) {
                    is SchemaMinItems -> put("minItems", annotation.value)
                    is SchemaMaxItems -> put("maxItems", annotation.value)
                    is UniqueItems -> put("uniqueItems", true)
                    is NotEmpty -> put("minItems", 1)
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                }
            }
        }
    }

    private fun inferMapSchema(
        valueType: KType?,
        annotations: List<Annotation>
    ): JsonElement {
        return buildJsonObject {
            put("type", "object")
            valueType?.let {
                put("additionalProperties", inferInternal(it, emptyList()))
            }
            annotations.forEach { annotation ->
                when (annotation) {
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun inferEnumSchema(klass: KClass<*>, annotations: List<Annotation>): JsonElement {
        val enumConstants = klass.java.enumConstants as Array<out Enum<*>>

        return buildJsonObject {
            put("type", "string")
            put("enum", buildJsonArray {
                enumConstants.forEach { constant ->
                    // Check for @SerialName on enum entry
                    val serialName = try {
                        klass.java.getDeclaredField(constant.name)
                            .getAnnotation(SerialName::class.java)?.value
                    } catch (_: NoSuchFieldException) {
                        null
                    } ?: constant.name
                    add(JsonPrimitive(serialName))
                }
            })
            annotations.forEach { annotation ->
                when (annotation) {
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                }
            }
        }
    }

    private fun inferSealedSchema(klass: KClass<*>, annotations: List<Annotation>): JsonElement {
        val subclasses = klass.sealedSubclasses

        return buildJsonObject {
            // Add class-level documentation
            klass.findAnnotation<SchemaTitle>()?.let { put("title", it.value) }
            klass.findAnnotation<SchemaDescription>()?.let { put("description", it.value) }

            // Add annotation-level documentation (property annotations override class)
            annotations.forEach { annotation ->
                when (annotation) {
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                }
            }

            put("oneOf", buildJsonArray {
                subclasses.forEach { subclass ->
                    // Check for recursion before processing subclass
                    if (subclass !in visitedTypes) {
                        add(inferFromClass(subclass))
                    }
                }
            })
        }
    }

    private fun inferDataClassSchema(klass: KClass<*>, annotations: List<Annotation>): JsonElement {
        // Check for recursion
        if (klass in visitedTypes) {
            // Return a reference-style placeholder for recursive types
            return buildJsonObject {
                put("\$comment", "Recursive reference to ${klass.simpleName}")
                put("type", "object")
            }
        }

        visitedTypes.add(klass)

        try {
            return inferFromClass(klass, annotations)
        } finally {
            visitedTypes.remove(klass)
        }
    }

    private fun inferFromClass(
        klass: KClass<*>,
        propertyAnnotations: List<Annotation> = emptyList()
    ): JsonElement {
        val constructor = klass.primaryConstructor
            ?: return buildJsonObject {
                put("type", "object")
            }

        val requiredProperties = mutableListOf<String>()
        val properties = mutableMapOf<String, JsonElement>()

        constructor.parameters.forEach { param ->
            @Suppress("UNCHECKED_CAST")
            val property = klass.memberProperties
                .find { it.name == param.name } as? KProperty1<Any, *>
                ?: return@forEach

            val propAnnotations = property.annotations
            val propType = param.type

            // Determine property name (respect @SerialName)
            val propertyName = property.findAnnotation<SerialName>()?.value
                ?: param.name
                ?: return@forEach

            // Check if required (non-nullable without default)
            if (!propType.isMarkedNullable && !param.isOptional) {
                requiredProperties.add(propertyName)
            }

            properties[propertyName] = inferInternal(propType, propAnnotations)
        }

        return buildJsonObject {
            put("type", "object")

            // Add class-level annotations
            klass.findAnnotation<SchemaTitle>()?.let { put("title", it.value) }
            klass.findAnnotation<SchemaDescription>()?.let { put("description", it.value) }

            // Property annotations can override class annotations
            propertyAnnotations.forEach { annotation ->
                when (annotation) {
                    is SchemaTitle -> put("title", annotation.value)
                    is SchemaDescription -> put("description", annotation.value)
                }
            }

            if (requiredProperties.isNotEmpty()) {
                put("required", buildJsonArray {
                    requiredProperties.forEach { add(JsonPrimitive(it)) }
                })
            }

            if (properties.isNotEmpty()) {
                put("properties", buildJsonObject {
                    properties.forEach { (name, schema) ->
                        put(name, schema)
                    }
                })
            }
        }
    }

    private fun wrapNullable(schema: JsonElement): JsonElement {
        return if (config.openApi30Nullable) {
            // OpenAPI 3.0 style: add "nullable": true
            buildJsonObject {
                if (schema is JsonObject) {
                    schema.forEach { (key, value) ->
                        put(key, value)
                    }
                }
                put("nullable", true)
            }
        } else {
            // JSON Schema style: anyOf with null
            buildJsonObject {
                put("anyOf", buildJsonArray {
                    add(schema)
                    add(buildJsonObject { put("type", "null") })
                })
            }
        }
    }
}

/**
 * Infers JSON Schema from a reified type parameter.
 *
 * Example:
 * ```kotlin
 * data class User(val name: String, val age: Int)
 * val schema = schemaOf<User>()
 * ```
 *
 * @param config Configuration options for inference
 * @return Schema representing the JSON Schema for the type
 */
inline fun <reified T> schemaOf(
    config: SchemaInferenceConfig = SchemaInferenceConfig()
): Schema {
    return SchemaInference(config).infer(typeOf<T>())
}

/**
 * Infers JSON Schema from a KClass.
 *
 * Example:
 * ```kotlin
 * val schema = schemaOf(User::class)
 * ```
 *
 * @param klass The class to infer schema from
 * @param config Configuration options for inference
 * @return Schema representing the JSON Schema for the class
 */
fun schemaOf(
    klass: KClass<*>,
    config: SchemaInferenceConfig = SchemaInferenceConfig()
): Schema {
    return SchemaInference(config).infer(klass)
}

/**
 * Infers JSON Schema from a KType.
 *
 * @param type The type to infer schema from
 * @param config Configuration options for inference
 * @return Schema representing the JSON Schema for the type
 */
fun schemaOf(
    type: KType,
    config: SchemaInferenceConfig = SchemaInferenceConfig()
): Schema {
    return SchemaInference(config).infer(type)
}
