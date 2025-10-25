package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Marker interface for all OpenAPI objects.
 *
 * This interface is implemented by all classes that represent
 * OpenAPI 3.2 specification objects.
 */
interface OpenAPIObject

/**
 * Interface for OpenAPI objects that support specification extensions.
 *
 * Specification extensions are properties with names starting with "x-".
 * They allow adding custom metadata to OpenAPI documents.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#specification-extensions">Specification Extensions</a>
 */
interface SupportsExtensions {
    /**
     * Map of specification extensions.
     * Keys must start with "x-".
     */
    val extensions: Map<String, Any?>?
        get() = null
}

/**
 * Sealed interface for OpenAPI objects that can be either a direct value
 * or a reference to a component.
 *
 * @param T the type of the direct value
 */
sealed interface Referenceable<out T : OpenAPIObject> : OpenAPIObject

/**
 * Represents a direct value (not a reference).
 *
 * @param T the type of the value
 * @property value the actual object value
 */
@JvmInline
value class Direct<out T : OpenAPIObject>(val value: T) : Referenceable<T>

/**
 * Represents a reference to a component.
 *
 * @property ref the reference string (e.g., "#/components/schemas/Pet")
 * @property summary optional summary of the referenced component
 * @property description optional description of the referenced component
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#reference-object">Reference Object</a>
 */
@Serializable
data class Reference(
    @SerialName("\$ref")
    val ref: String,
    val summary: String? = null,
    val description: String? = null,
) : Referenceable<Nothing>, OpenAPIObject