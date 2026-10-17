package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Contact information for the exposed API.
 *
 * @property name The identifying name of the contact person/organization
 * @property url The URL pointing to the contact information (must be URI-reference format)
 * @property email The email address of the contact person/organization (must be email format)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#contact-object">Contact Object</a>
 */
@Serializable
data class Contact(
    val name: String? = null,
    val url: String? = null,
    val email: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions
