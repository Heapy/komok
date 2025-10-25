package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * License information for the exposed API.
 *
 * Note: identifier and url are mutually exclusive. Only one should be specified.
 *
 * @property name The license name used for the API (required)
 * @property identifier An SPDX license expression. Mutually exclusive with url
 * @property url A URL to the license used for the API (must be URI-reference format). Mutually exclusive with identifier
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#license-object">License Object</a>
 */
@Serializable
data class License(
    val name: String,
    val identifier: String? = null,
    val url: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        require(identifier == null || url == null) {
            "License identifier and url are mutually exclusive. Only one should be specified."
        }
    }
}
