package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Each Media Type Object provides schema and examples for the media type identified by its key.
 *
 * Note: The encoding property and (prefixEncoding or itemEncoding) are mutually exclusive.
 * Note: example and examples are mutually exclusive.
 *
 * @property description A description of the media type
 * @property schema The schema defining the content
 * @property itemSchema The schema for items in array-like structures
 * @property encoding A map of property names to their encoding information (mutually exclusive with prefixEncoding/itemEncoding)
 * @property prefixEncoding Array of encoding objects for prefix items (mutually exclusive with encoding)
 * @property itemEncoding Encoding object for items (mutually exclusive with encoding)
 * @property example Example of the media type (mutually exclusive with examples)
 * @property examples Map of examples for the media type (mutually exclusive with example)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#media-type-object">Media Type Object</a>
 */
@Serializable
data class MediaType(
    val description: String? = null,
    val schema: Schema? = null,
    val itemSchema: Schema? = null,
    val encoding: Map<String, Encoding>? = null,
    val prefixEncoding: List<Encoding>? = null,
    val itemEncoding: Encoding? = null,
    val example: JsonElement? = null,
    val examples: Map<String, Example>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate mutual exclusivity: encoding and (prefixEncoding or itemEncoding)
        if (encoding != null) {
            require(prefixEncoding == null && itemEncoding == null) {
                "MediaType 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'"
            }
        }

        // Validate mutual exclusivity: example and examples
        require(!(example != null && examples != null)) {
            "MediaType 'example' and 'examples' are mutually exclusive"
        }
    }
}

/**
 * Type alias for Content, which is a map of media type strings to MediaType objects.
 *
 * The keys should be media type names (e.g., "application/json", "text/plain").
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#fixed-fields-10">Content</a>
 */
typealias Content = Map<String, MediaType>
