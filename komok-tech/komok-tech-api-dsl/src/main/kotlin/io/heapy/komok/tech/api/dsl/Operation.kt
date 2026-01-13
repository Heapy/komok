package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Describes a single API operation on a path.
 *
 * Note: Callbacks reference PathItem, which creates a circular dependency, so callback paths are PathItem objects.
 *
 * @property tags A list of tags for API documentation control
 * @property summary A short summary of what the operation does
 * @property description A verbose explanation of the operation behavior
 * @property externalDocs Additional external documentation for this operation
 * @property operationId Unique string used to identify the operation
 * @property parameters A list of parameters that are applicable for this operation
 * @property requestBody The request body applicable for this operation
 * @property responses The list of possible responses as they are returned from executing this operation (required)
 * @property callbacks A map of possible out-of band callbacks related to the parent operation
 * @property deprecated Declares this operation to be deprecated (default: false)
 * @property security A declaration of which security mechanisms can be used for this operation
 * @property servers An alternative server array to service this operation
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#operation-object">Operation Object</a>
 */
@Serializable
data class Operation(
    val responses: Responses,
    val tags: List<String>? = null,
    val summary: String? = null,
    val description: String? = null,
    val externalDocs: ExternalDocumentation? = null,
    val operationId: String? = null,
    @Serializable(with = ReferenceableParameterListSerializer::class)
    val parameters: List<Referenceable<Parameter>>? = null,
    val requestBody: RequestBody? = null,
    val callbacks: Map<String, Callback>? = null,
    val deprecated: Boolean = false,
    val security: List<SecurityRequirement>? = null,
    val servers: List<Server>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        require(responses.isNotEmpty()) {
            "Operation must have at least one response defined"
        }
    }
}

/**
 * A map of possible out-of band callbacks related to the parent operation.
 *
 * Each value in the map is a Path Item Object that describes a set of requests
 * that may be initiated by the API provider and the expected responses.
 * The key value used to identify the path item object is an expression,
 * evaluated at runtime, that identifies a URL to use for the callback operation.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#callback-object">Callback Object</a>
 */
typealias Callback = Map<String, PathItem>
