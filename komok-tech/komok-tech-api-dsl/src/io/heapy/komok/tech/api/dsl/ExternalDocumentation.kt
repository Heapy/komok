package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Allows referencing an external resource for extended documentation.
 *
 * @property description A description of the target documentation
 * @property url The URL for the target documentation (required, must be URI-reference format)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#external-documentation-object">External Documentation Object</a>
 */
@Serializable
data class ExternalDocumentation(
    val description: String? = null,
    val url: String,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions
