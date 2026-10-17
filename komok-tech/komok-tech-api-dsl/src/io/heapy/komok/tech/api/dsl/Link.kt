package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a possible design-time link for a response.
 *
 * The presence of a link does not guarantee the caller's ability to successfully invoke it,
 * rather it provides a known relationship and traversal mechanism between responses and other operations.
 *
 * Unlike dynamic links (i.e. links provided in the response payload), the OAS linking mechanism
 * does not require link information in the runtime response.
 *
 * For computing links, and providing instructions to execute them, a runtime expression is used
 * for accessing values in an operation and using them as parameters while invoking the linked operation.
 *
 * @property operationRef A relative or absolute URI reference to an OAS operation (mutually exclusive with operationId)
 * @property operationId The name of an existing, resolvable OAS operation (mutually exclusive with operationRef)
 * @property parameters A map representing parameters to pass to an operation as specified with operationId or identified via operationRef
 * @property requestBody A literal value or expression to use as a request body when calling the target operation
 * @property description A description of the link
 * @property server A server object to be used by the target operation
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#link-object">Link Object</a>
 */
@Serializable
data class Link(
    val operationRef: String? = null,
    val operationId: String? = null,
    val parameters: Map<String, String>? = null,
    val requestBody: JsonElement? = null,
    val description: String? = null,
    val server: Server? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate that exactly one of operationRef or operationId is present
        require((operationRef != null) xor (operationId != null)) {
            "Link must have exactly one of 'operationRef' or 'operationId' specified"
        }
    }
}
