package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The Header Object follows the structure of the Parameter Object.
 *
 * Note: schema and content are mutually exclusive - exactly one must be present.
 *
 * @property description A brief description of the header
 * @property required Determines whether this header is mandatory
 * @property deprecated Specifies that the header is deprecated
 * @property schema The schema defining the type used for the header (mutually exclusive with content)
 * @property content A map containing the representations for the header (mutually exclusive with schema)
 * @property style Describes how the header value will be serialized
 * @property explode When true, generates separate parameters for array or object values
 * @property example Example of the header's potential value
 * @property examples Examples of the header's potential value
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#header-object">Header Object</a>
 */
@Serializable
data class Header(
    val description: String? = null,
    val required: Boolean = false,
    val deprecated: Boolean = false,
    val schema: Schema? = null,
    val content: Map<String, MediaType>? = null,
    val style: String? = null,
    val explode: Boolean? = null,
    val example: JsonElement? = null,
    val examples: Map<String, Example>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate that exactly one of schema or content is present
        val hasSchema = schema != null
        val hasContent = content != null && content.isNotEmpty()

        require(hasSchema xor hasContent) {
            "Header must have exactly one of 'schema' or 'content' specified"
        }

        // Validate that example and examples are mutually exclusive
        require(!(example != null && examples != null)) {
            "Header 'example' and 'examples' are mutually exclusive"
        }
    }
}
