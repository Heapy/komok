package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * An object representing a Server.
 *
 * @property url A URL to the target host (required). This URL supports Server Variables and may be relative,
 *                to indicate that the host location is relative to the location where the OpenAPI document is being served.
 * @property description An optional string describing the host designated by the URL
 * @property name An optional name identifying the server
 * @property variables A map between a variable name and its value. The value is used for substitution in the server's URL template
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#server-object">Server Object</a>
 */
@Serializable
data class Server(
    val url: String,
    val description: String? = null,
    val name: String? = null,
    val variables: Map<String, ServerVariable>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions
