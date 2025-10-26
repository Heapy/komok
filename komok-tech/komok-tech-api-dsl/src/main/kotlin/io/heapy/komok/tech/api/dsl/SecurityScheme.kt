package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Defines a security scheme that can be used by the operations.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#security-scheme-object">Security Scheme Object</a>
 */
@Serializable
data class SecurityScheme(
    val type: SecuritySchemeType,
    val description: String? = null,
    val deprecated: Boolean = false,
    // API Key fields
    val name: String? = null,
    @SerialName("in")
    val location: ApiKeyLocation? = null,
    // HTTP fields
    val scheme: String? = null,
    val bearerFormat: String? = null,
    // OAuth2 fields
    val flows: OAuthFlows? = null,
    val oauth2MetadataUrl: String? = null,
    // OpenID Connect fields
    val openIdConnectUrl: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        when (type) {
            SecuritySchemeType.API_KEY -> {
                require(name != null) { "API Key security scheme must have 'name' specified" }
                require(location != null) { "API Key security scheme must have 'in' location specified" }
            }
            SecuritySchemeType.HTTP -> {
                require(scheme != null) { "HTTP security scheme must have 'scheme' specified" }
            }
            SecuritySchemeType.MUTUAL_TLS -> {
                // No required fields
            }
            SecuritySchemeType.OAUTH2 -> {
                require(flows != null) { "OAuth2 security scheme must have 'flows' specified" }
            }
            SecuritySchemeType.OPEN_ID_CONNECT -> {
                require(openIdConnectUrl != null) { "OpenID Connect security scheme must have 'openIdConnectUrl' specified" }
            }
        }
    }

    companion object {
        /**
         * Creates an API Key security scheme.
         */
        fun apiKey(
            name: String,
            location: ApiKeyLocation,
            description: String? = null,
            deprecated: Boolean = false,
            extensions: Map<String, JsonElement>? = null,
        ) = SecurityScheme(
            type = SecuritySchemeType.API_KEY,
            name = name,
            location = location,
            description = description,
            deprecated = deprecated,
            extensions = extensions
        )

        /**
         * Creates an HTTP authentication security scheme.
         */
        fun http(
            scheme: String,
            bearerFormat: String? = null,
            description: String? = null,
            deprecated: Boolean = false,
            extensions: Map<String, JsonElement>? = null,
        ) = SecurityScheme(
            type = SecuritySchemeType.HTTP,
            scheme = scheme,
            bearerFormat = bearerFormat,
            description = description,
            deprecated = deprecated,
            extensions = extensions
        )

        /**
         * Creates a Mutual TLS security scheme.
         */
        fun mutualTLS(
            description: String? = null,
            deprecated: Boolean = false,
            extensions: Map<String, JsonElement>? = null,
        ) = SecurityScheme(
            type = SecuritySchemeType.MUTUAL_TLS,
            description = description,
            deprecated = deprecated,
            extensions = extensions
        )

        /**
         * Creates an OAuth 2.0 security scheme.
         */
        fun oauth2(
            flows: OAuthFlows,
            oauth2MetadataUrl: String? = null,
            description: String? = null,
            deprecated: Boolean = false,
            extensions: Map<String, JsonElement>? = null,
        ) = SecurityScheme(
            type = SecuritySchemeType.OAUTH2,
            flows = flows,
            oauth2MetadataUrl = oauth2MetadataUrl,
            description = description,
            deprecated = deprecated,
            extensions = extensions
        )

        /**
         * Creates an OpenID Connect security scheme.
         */
        fun openIdConnect(
            openIdConnectUrl: String,
            description: String? = null,
            deprecated: Boolean = false,
            extensions: Map<String, JsonElement>? = null,
        ) = SecurityScheme(
            type = SecuritySchemeType.OPEN_ID_CONNECT,
            openIdConnectUrl = openIdConnectUrl,
            description = description,
            deprecated = deprecated,
            extensions = extensions
        )
    }
}

/**
 * The type of the security scheme.
 */
@Serializable
enum class SecuritySchemeType {
    @SerialName("apiKey")
    API_KEY,

    @SerialName("http")
    HTTP,

    @SerialName("mutualTLS")
    MUTUAL_TLS,

    @SerialName("oauth2")
    OAUTH2,

    @SerialName("openIdConnect")
    OPEN_ID_CONNECT,
}

/**
 * The location of the API key.
 */
@Serializable
enum class ApiKeyLocation {
    @SerialName("query")
    QUERY,

    @SerialName("header")
    HEADER,

    @SerialName("cookie")
    COOKIE,
}

/**
 * Configuration details for a supported OAuth flow.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#oauth-flows-object">OAuth Flows Object</a>
 */
@Serializable
data class OAuthFlows(
    val implicit: OAuthFlow.Implicit? = null,
    val password: OAuthFlow.Password? = null,
    val clientCredentials: OAuthFlow.ClientCredentials? = null,
    val authorizationCode: OAuthFlow.AuthorizationCode? = null,
    val deviceAuthorization: OAuthFlow.DeviceAuthorization? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions

/**
 * Configuration details for a supported OAuth Flow.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#oauth-flow-object">OAuth Flow Object</a>
 */
sealed interface OAuthFlow : OpenAPIObject, SupportsExtensions {
    val refreshUrl: String?
    val scopes: Map<String, String>

    /**
     * Implicit OAuth flow.
     *
     * @property authorizationUrl The authorization URL to be used for this flow (required)
     * @property refreshUrl The URL to be used for obtaining refresh tokens
     * @property scopes The available scopes for the OAuth2 security scheme (required)
     */
    @Serializable
    data class Implicit(
        val authorizationUrl: String,
        override val refreshUrl: String? = null,
        override val scopes: Map<String, String>,
        override val extensions: Map<String, JsonElement>? = null,
    ) : OAuthFlow

    /**
     * Resource Owner Password Credentials flow (also known as Password flow).
     *
     * @property tokenUrl The token URL to be used for this flow (required)
     * @property refreshUrl The URL to be used for obtaining refresh tokens
     * @property scopes The available scopes for the OAuth2 security scheme (required)
     */
    @Serializable
    data class Password(
        val tokenUrl: String,
        override val refreshUrl: String? = null,
        override val scopes: Map<String, String>,
        override val extensions: Map<String, JsonElement>? = null,
    ) : OAuthFlow

    /**
     * Client Credentials flow (also known as Application flow).
     *
     * @property tokenUrl The token URL to be used for this flow (required)
     * @property refreshUrl The URL to be used for obtaining refresh tokens
     * @property scopes The available scopes for the OAuth2 security scheme (required)
     */
    @Serializable
    data class ClientCredentials(
        val tokenUrl: String,
        override val refreshUrl: String? = null,
        override val scopes: Map<String, String>,
        override val extensions: Map<String, JsonElement>? = null,
    ) : OAuthFlow

    /**
     * Authorization Code flow.
     *
     * @property authorizationUrl The authorization URL to be used for this flow (required)
     * @property tokenUrl The token URL to be used for this flow (required)
     * @property refreshUrl The URL to be used for obtaining refresh tokens
     * @property scopes The available scopes for the OAuth2 security scheme (required)
     */
    @Serializable
    data class AuthorizationCode(
        val authorizationUrl: String,
        val tokenUrl: String,
        override val refreshUrl: String? = null,
        override val scopes: Map<String, String>,
        override val extensions: Map<String, JsonElement>? = null,
    ) : OAuthFlow

    /**
     * Device Authorization flow (OAuth 2.0 Device Authorization Grant).
     *
     * @property deviceAuthorizationUrl The device authorization URL to be used for this flow (required)
     * @property tokenUrl The token URL to be used for this flow (required)
     * @property refreshUrl The URL to be used for obtaining refresh tokens
     * @property scopes The available scopes for the OAuth2 security scheme (required)
     */
    @Serializable
    data class DeviceAuthorization(
        val deviceAuthorizationUrl: String,
        val tokenUrl: String,
        override val refreshUrl: String? = null,
        override val scopes: Map<String, String>,
        override val extensions: Map<String, JsonElement>? = null,
    ) : OAuthFlow
}
