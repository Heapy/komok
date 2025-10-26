package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Provides metadata about the API.
 *
 * The metadata MAY be used by the clients if needed, and MAY be presented in editing or
 * documentation generation tools for convenience.
 *
 * @property title The title of the API (required)
 * @property version The version of the OpenAPI document (required, not the OpenAPI Specification version)
 * @property summary A short summary of the API
 * @property description A description of the API (CommonMark syntax MAY be used for rich text representation)
 * @property termsOfService A URL to the Terms of Service for the API
 * @property contact The contact information for the exposed API
 * @property license The license information for the exposed API
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#info-object">Info Object</a>
 */
@Serializable
data class Info(
    val title: String,
    val version: String,
    val summary: String? = null,
    val description: String? = null,
    val termsOfService: String? = null,
    val contact: Contact? = null,
    val license: License? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions
