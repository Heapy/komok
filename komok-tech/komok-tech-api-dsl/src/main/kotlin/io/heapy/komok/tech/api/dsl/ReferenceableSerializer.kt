package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * A custom serializer for [Referenceable] that handles polymorphic serialization
 * between [Direct] (inline objects) and [Reference] (`$ref` pointers).
 *
 * During serialization:
 * - [Direct] values are serialized as their unwrapped inner value
 * - [Reference] values are serialized as `{"$ref": "...", "summary": "...", "description": "..."}`
 *
 * During deserialization:
 * - JSON objects containing `$ref` are deserialized as [Reference]
 * - All other JSON objects are deserialized as [Direct] wrapping the actual type
 *
 * @param T the type of the direct value, must extend [OpenAPIObject]
 * @param valueSerializer the serializer for the direct value type T
 *
 * @see Referenceable
 * @see Direct
 * @see Reference
 */
class ReferenceableSerializer<T : OpenAPIObject>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<Referenceable<T>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Referenceable<${valueSerializer.descriptor.serialName}>")

    override fun serialize(encoder: Encoder, value: Referenceable<T>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalArgumentException(
                "ReferenceableSerializer only supports JSON encoding. " +
                    "Got: ${encoder::class.simpleName}"
            )

        when (value) {
            is Direct -> {
                // Serialize the unwrapped value directly
                jsonEncoder.encodeSerializableValue(valueSerializer, value.value)
            }
            is Reference -> {
                // Serialize as Reference object
                jsonEncoder.encodeSerializableValue(Reference.serializer(), value)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Referenceable<T> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalArgumentException(
                "ReferenceableSerializer only supports JSON decoding. " +
                    "Got: ${decoder::class.simpleName}"
            )

        val jsonElement = jsonDecoder.decodeJsonElement()

        // Check if this is a reference (has $ref field)
        if (jsonElement is JsonObject && REF_KEY in jsonElement) {
            // Deserialize as Reference
            return jsonDecoder.json.decodeFromJsonElement(Reference.serializer(), jsonElement)
        }

        // Otherwise, deserialize as Direct<T>
        val value = jsonDecoder.json.decodeFromJsonElement(valueSerializer, jsonElement)
        return Direct(value)
    }

    companion object {
        private const val REF_KEY = "\$ref"
    }
}

/**
 * Creates a [ReferenceableSerializer] for the given value type.
 *
 * Usage:
 * ```kotlin
 * @Serializable
 * data class MyClass(
 *     @Serializable(with = ReferenceableSerializer::class)
 *     val header: Referenceable<Header>
 * )
 * ```
 *
 * Or for map values:
 * ```kotlin
 * val serializer = ReferenceableSerializer(Header.serializer())
 * ```
 *
 * @param T the type of the direct value
 * @param valueSerializer the serializer for type T
 * @return a serializer for Referenceable<T>
 */
fun <T : OpenAPIObject> referenceableSerializer(
    valueSerializer: KSerializer<T>,
): KSerializer<Referenceable<T>> = ReferenceableSerializer(valueSerializer)

/**
 * Concrete serializer for [Referenceable]<[Header]>.
 *
 * Use this with `@Serializable(with = ReferenceableHeaderSerializer::class)` annotation.
 */
object ReferenceableHeaderSerializer : KSerializer<Referenceable<Header>>
    by ReferenceableSerializer(Header.serializer())

/**
 * Concrete serializer for [Referenceable]<[Example]>.
 *
 * Use this with `@Serializable(with = ReferenceableExampleSerializer::class)` annotation.
 */
object ReferenceableExampleSerializer : KSerializer<Referenceable<Example>>
    by ReferenceableSerializer(Example.serializer())

/**
 * Concrete serializer for [Referenceable]<[Parameter]>.
 *
 * Use this with `@Serializable(with = ReferenceableParameterSerializer::class)` annotation.
 */
object ReferenceableParameterSerializer : KSerializer<Referenceable<Parameter>>
    by ReferenceableSerializer(Parameter.serializer())

/**
 * Concrete serializer for [Referenceable]<[RequestBody]>.
 *
 * Use this with `@Serializable(with = ReferenceableRequestBodySerializer::class)` annotation.
 */
object ReferenceableRequestBodySerializer : KSerializer<Referenceable<RequestBody>>
    by ReferenceableSerializer(RequestBody.serializer())

/**
 * Concrete serializer for [Referenceable]<[Link]>.
 *
 * Use this with `@Serializable(with = ReferenceableLinkSerializer::class)` annotation.
 */
object ReferenceableLinkSerializer : KSerializer<Referenceable<Link>>
    by ReferenceableSerializer(Link.serializer())

/**
 * Concrete serializer for [Referenceable]<[Response]>.
 *
 * Use this with `@Serializable(with = ReferenceableResponseSerializer::class)` annotation.
 */
object ReferenceableResponseSerializer : KSerializer<Referenceable<Response>>
    by ReferenceableSerializer(Response.serializer())

/**
 * Concrete serializer for [Referenceable]<[SecurityScheme]>.
 *
 * Use this with `@Serializable(with = ReferenceableSecuritySchemeSerializer::class)` annotation.
 */
object ReferenceableSecuritySchemeSerializer : KSerializer<Referenceable<SecurityScheme>>
    by ReferenceableSerializer(SecurityScheme.serializer())

// Map Serializers for common patterns

/**
 * Serializer for `Map<String, Referenceable<Header>>`.
 *
 * Use this with `@Serializable(with = ReferenceableHeaderMapSerializer::class)` annotation.
 */
object ReferenceableHeaderMapSerializer : KSerializer<Map<String, Referenceable<Header>>>
    by MapSerializer(String.serializer(), ReferenceableHeaderSerializer)

/**
 * Serializer for `Map<String, Referenceable<Example>>`.
 *
 * Use this with `@Serializable(with = ReferenceableExampleMapSerializer::class)` annotation.
 */
object ReferenceableExampleMapSerializer : KSerializer<Map<String, Referenceable<Example>>>
    by MapSerializer(String.serializer(), ReferenceableExampleSerializer)

/**
 * Serializer for `Map<String, Referenceable<Link>>`.
 *
 * Use this with `@Serializable(with = ReferenceableLinkMapSerializer::class)` annotation.
 */
object ReferenceableLinkMapSerializer : KSerializer<Map<String, Referenceable<Link>>>
    by MapSerializer(String.serializer(), ReferenceableLinkSerializer)

/**
 * Serializer for `Map<String, Referenceable<Response>>`.
 *
 * Use this with `@Serializable(with = ReferenceableResponseMapSerializer::class)` annotation.
 */
object ReferenceableResponseMapSerializer : KSerializer<Map<String, Referenceable<Response>>>
    by MapSerializer(String.serializer(), ReferenceableResponseSerializer)

