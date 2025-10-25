package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Adds metadata to a single tag that is used by the Operation Object.
 *
 * @property name The name of the tag (required)
 * @property summary A short summary of the tag
 * @property description A description for the tag
 * @property externalDocs Additional external documentation for this tag
 * @property parent The name of a parent tag
 * @property kind A hint about the purpose or audience of the tag
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#tag-object">Tag Object</a>
 */
@Serializable
data class Tag(
    val name: String,
    val summary: String? = null,
    val description: String? = null,
    val externalDocs: ExternalDocumentation? = null,
    val parent: String? = null,
    val kind: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions
