package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The root object of the OpenAPI document.
 *
 * This is the entry point for an OpenAPI definition. It describes the API, its operations,
 * authentication methods, and other metadata.
 *
 * @property openapi The OpenAPI Specification version that the document uses (required, must match pattern ^3\.2\.\d+(-.+)?$)
 * @property info Provides metadata about the API (required)
 * @property jsonSchemaDialect The default JSON Schema dialect for Schema Objects (default: https://spec.openapis.org/oas/3.2/dialect/2025-09-17)
 * @property servers An array of Server Objects providing connectivity information to a target server (default: [Server(url = "/")])
 * @property paths The available paths and operations for the API
 * @property webhooks The incoming webhooks that MAY be received as part of this API
 * @property components An element to hold various reusable objects for the specification
 * @property security A declaration of which security mechanisms can be used across the API
 * @property tags A list of tags used by the document with additional metadata
 * @property externalDocs Additional external documentation
 * @property self A URI reference to the OpenAPI document itself (must not contain a fragment)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#openapi-object">OpenAPI Object</a>
 */
@Serializable
data class OpenAPI(
    val openapi: String,
    val info: Info,
    val jsonSchemaDialect: String? = null,
    val servers: List<Server>? = null,
    val paths: Paths? = null,
    val webhooks: Map<String, PathItem>? = null,
    val components: Components? = null,
    val security: List<SecurityRequirement>? = null,
    val tags: List<Tag>? = null,
    val externalDocs: ExternalDocumentation? = null,
    @SerialName("\$self")
    val self: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate OpenAPI version pattern: ^3\.2\.\d+(-.+)?$
        require(openapi.matches(Regex("^3\\.2\\.\\d+(-.+)?\$"))) {
            "OpenAPI version '$openapi' must match pattern ^3\\.2\\.\\d+(-.+)?\$. Examples: 3.2.0, 3.2.1, 3.2.0-rc1"
        }

        // Validate that at least one of paths, components, or webhooks is present
        require(paths != null || components != null || webhooks != null) {
            "OpenAPI document must have at least one of 'paths', 'components', or 'webhooks' defined"
        }

        // Validate that $self does not contain a fragment
        if (self != null) {
            require(!self.contains('#')) {
                "OpenAPI '\$self' property must not contain a fragment (#). Got: $self"
            }
        }
    }

    companion object {
        /**
         * The current OpenAPI specification version.
         */
        const val VERSION_3_2_0 = "3.2.0"

        /**
         * The default JSON Schema dialect URL for OpenAPI 3.2.
         */
        const val DEFAULT_JSON_SCHEMA_DIALECT = "https://spec.openapis.org/oas/3.2/dialect/2025-09-17"
    }
}
