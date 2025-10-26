package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Describes a single request body.
 *
 * @property description A brief description of the request body
 * @property content The content of the request body (required). Maps media type to MediaType object
 * @property required Determines if the request body is required in the request (default: false)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#request-body-object">Request Body Object</a>
 */
@Serializable
data class RequestBody(
    val content: Content,
    val description: String? = null,
    val required: Boolean = false,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        require(content.isNotEmpty()) {
            "RequestBody 'content' must not be empty"
        }
    }
}
