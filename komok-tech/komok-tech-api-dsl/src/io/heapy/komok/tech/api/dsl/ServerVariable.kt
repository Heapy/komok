package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * An object representing a Server Variable for server URL template substitution.
 *
 * @property enum An enumeration of string values to be used if the substitution options are from a limited set (minimum 1 value)
 * @property default The default value to use for substitution (required)
 * @property description An optional description for the server variable
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#server-variable-object">Server Variable Object</a>
 */
@Serializable
data class ServerVariable(
    val default: String,
    val enum: List<String>? = null,
    val description: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        if (enum != null) {
            require(enum.isNotEmpty()) {
                "ServerVariable enum must contain at least one value"
            }
        }
    }
}
