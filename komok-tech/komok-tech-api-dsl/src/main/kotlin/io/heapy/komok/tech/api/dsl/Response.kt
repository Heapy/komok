package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Describes a single response from an API Operation.
 *
 * Note: Links are not yet implemented (deferred to later phase).
 *
 * @property description A description of the response
 * @property summary A short summary of the response
 * @property headers Maps a header name to its definition
 * @property content A map containing descriptions of potential response payloads
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#response-object">Response Object</a>
 */
@Serializable
data class Response(
    val description: String? = null,
    val summary: String? = null,
    val headers: Map<String, Header>? = null,
    val content: Content? = null,
    // TODO: Add links support in later phase
    // val links: Map<String, Referenceable<Link>>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions

/**
 * A container for the expected responses of an operation.
 * The container maps a HTTP response code or "default" to the expected response.
 *
 * Status codes can be:
 * - Specific: 200, 201, 404, etc.
 * - Wildcard patterns: 1XX, 2XX, 3XX, 4XX, 5XX
 * - default: for responses that don't match any specific code
 *
 * At least one response (either a status code or default) must be defined.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#responses-object">Responses Object</a>
 */
typealias Responses = Map<String, Response>

/**
 * Creates a Responses map from vararg pairs.
 *
 * @param pairs key-value pairs where keys are status codes (e.g., "200", "2XX", "default")
 * @return map of responses
 * @throws IllegalArgumentException if no responses are provided or if status codes are invalid
 */
fun responses(vararg pairs: Pair<String, Response>): Responses {
    require(pairs.isNotEmpty()) {
        "Responses must contain at least one response"
    }

    pairs.forEach { (statusCode, _) ->
        // Validate status code format: specific codes (200-599), wildcard patterns (1XX-5XX), or "default"
        val isValid = when {
            statusCode == "default" -> true
            statusCode.matches(Regex("^[1-5]XX$")) -> true // Wildcard patterns: 1XX, 2XX, 3XX, 4XX, 5XX
            statusCode.matches(Regex("^[1-5][0-9]{2}$")) -> { // Specific codes: 100-599
                val code = statusCode.toIntOrNull()
                code != null && code in 100..599
            }
            else -> false
        }

        require(isValid) {
            "Invalid status code: '$statusCode'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'"
        }
    }

    return mapOf(*pairs)
}
