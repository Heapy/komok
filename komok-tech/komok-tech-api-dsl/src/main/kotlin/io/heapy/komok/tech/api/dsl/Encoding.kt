package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A single encoding definition applied to a single schema property.
 *
 * Note: The encoding property and (prefixEncoding or itemEncoding) are mutually exclusive.
 *
 * @property contentType The Content-Type for encoding a specific property (media-range format)
 * @property headers A map allowing additional information to be provided as headers
 * @property style Describes how a specific property value will be serialized
 * @property explode When true, property values of type array or object generate separate parameters
 * @property allowReserved Determines whether reserved characters are allowed in the parameter value
 * @property encoding A map of encoding objects (mutually exclusive with prefixEncoding and itemEncoding)
 * @property prefixEncoding Array of encoding objects for prefix items (mutually exclusive with encoding)
 * @property itemEncoding Encoding object for items (mutually exclusive with encoding)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#encoding-object">Encoding Object</a>
 */
@Serializable
data class Encoding(
    val contentType: String? = null,
    // Temporarily removed: val headers: Map<String, Referenceable<Header>>? = null,
    val style: EncodingStyle? = null,
    val explode: Boolean? = null,
    val allowReserved: Boolean? = null,
    val encoding: Map<String, Encoding>? = null,
    val prefixEncoding: List<Encoding>? = null,
    val itemEncoding: Encoding? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate mutual exclusivity: encoding and (prefixEncoding or itemEncoding)
        if (encoding != null) {
            require(prefixEncoding == null && itemEncoding == null) {
                "Encoding 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'"
            }
        }
    }
}

/**
 * Encoding style values for serialization.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#encoding-object">Encoding Object</a>
 */
@Serializable
enum class EncodingStyle {
    @kotlinx.serialization.SerialName("form")
    FORM,

    @kotlinx.serialization.SerialName("spaceDelimited")
    SPACE_DELIMITED,

    @kotlinx.serialization.SerialName("pipeDelimited")
    PIPE_DELIMITED,

    @kotlinx.serialization.SerialName("deepObject")
    DEEP_OBJECT,
}
