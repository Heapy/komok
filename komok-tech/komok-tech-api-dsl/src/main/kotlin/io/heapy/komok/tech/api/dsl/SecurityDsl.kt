package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

// ============================================
// OAuth Scopes DSL
// ============================================

/**
 * DSL builder for OAuth scopes map.
 *
 * Example usage:
 * ```kotlin
 * val scopes = oauthScopes {
 *     "read:pets" description "Read your pets"
 *     "write:pets" description "Modify pets in your account"
 * }
 * ```
 */
class OAuthScopesBuilder {
    @PublishedApi
    internal val scopes = mutableMapOf<String, String>()

    /**
     * Adds a scope with a description using infix syntax.
     */
    infix fun String.description(description: String) {
        scopes[this] = description
    }

    fun build(): Map<String, String> = scopes.toMap()
}

/**
 * Creates an OAuth scopes map using DSL syntax.
 *
 * @param block configuration block for the scopes
 * @return map of scope names to descriptions
 */
inline fun oauthScopes(block: OAuthScopesBuilder.() -> Unit): Map<String, String> {
    return OAuthScopesBuilder().apply(block).build()
}

// ============================================
// OAuth Flow DSL Builders
// ============================================

/**
 * DSL builder for [OAuthFlow.Implicit].
 *
 * The builder enforces fail-fast validation:
 * - `authorizationUrl` is required
 * - `scopes` is required
 *
 * Example usage:
 * ```kotlin
 * val flow = implicitFlow {
 *     authorizationUrl = "https://example.com/oauth/authorize"
 *     refreshUrl = "https://example.com/oauth/refresh"
 *     scopes {
 *         "read:pets" description "Read your pets"
 *         "write:pets" description "Modify pets"
 *     }
 * }
 * ```
 */
class ImplicitFlowBuilder {
    var authorizationUrl: String? = null
    var refreshUrl: String? = null
    var scopes: Map<String, String>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures scopes using DSL syntax.
     */
    inline fun scopes(block: OAuthScopesBuilder.() -> Unit) {
        scopes = oauthScopes(block)
    }

    fun build(): OAuthFlow.Implicit {
        val flowAuthorizationUrl = requireNotNull(authorizationUrl) {
            "Implicit flow authorizationUrl is required"
        }
        val flowScopes = requireNotNull(scopes) {
            "Implicit flow scopes is required"
        }
        return OAuthFlow.Implicit(
            authorizationUrl = flowAuthorizationUrl,
            refreshUrl = refreshUrl,
            scopes = flowScopes,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [OAuthFlow.Implicit] using DSL syntax.
 *
 * @param block configuration block for the implicit flow
 * @return configured OAuthFlow.Implicit object
 * @throws IllegalArgumentException if authorizationUrl or scopes is not provided
 */
inline fun implicitFlow(block: ImplicitFlowBuilder.() -> Unit): OAuthFlow.Implicit {
    return ImplicitFlowBuilder().apply(block).build()
}

/**
 * DSL builder for [OAuthFlow.Password].
 *
 * The builder enforces fail-fast validation:
 * - `tokenUrl` is required
 * - `scopes` is required
 *
 * Example usage:
 * ```kotlin
 * val flow = passwordFlow {
 *     tokenUrl = "https://example.com/oauth/token"
 *     scopes {
 *         "read:users" description "Read user data"
 *     }
 * }
 * ```
 */
class PasswordFlowBuilder {
    var tokenUrl: String? = null
    var refreshUrl: String? = null
    var scopes: Map<String, String>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures scopes using DSL syntax.
     */
    inline fun scopes(block: OAuthScopesBuilder.() -> Unit) {
        scopes = oauthScopes(block)
    }

    fun build(): OAuthFlow.Password {
        val flowTokenUrl = requireNotNull(tokenUrl) {
            "Password flow tokenUrl is required"
        }
        val flowScopes = requireNotNull(scopes) {
            "Password flow scopes is required"
        }
        return OAuthFlow.Password(
            tokenUrl = flowTokenUrl,
            refreshUrl = refreshUrl,
            scopes = flowScopes,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [OAuthFlow.Password] using DSL syntax.
 *
 * @param block configuration block for the password flow
 * @return configured OAuthFlow.Password object
 * @throws IllegalArgumentException if tokenUrl or scopes is not provided
 */
inline fun passwordFlow(block: PasswordFlowBuilder.() -> Unit): OAuthFlow.Password {
    return PasswordFlowBuilder().apply(block).build()
}

/**
 * DSL builder for [OAuthFlow.ClientCredentials].
 *
 * The builder enforces fail-fast validation:
 * - `tokenUrl` is required
 * - `scopes` is required
 *
 * Example usage:
 * ```kotlin
 * val flow = clientCredentialsFlow {
 *     tokenUrl = "https://example.com/oauth/token"
 *     scopes {
 *         "admin" description "Full admin access"
 *     }
 * }
 * ```
 */
class ClientCredentialsFlowBuilder {
    var tokenUrl: String? = null
    var refreshUrl: String? = null
    var scopes: Map<String, String>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures scopes using DSL syntax.
     */
    inline fun scopes(block: OAuthScopesBuilder.() -> Unit) {
        scopes = oauthScopes(block)
    }

    fun build(): OAuthFlow.ClientCredentials {
        val flowTokenUrl = requireNotNull(tokenUrl) {
            "ClientCredentials flow tokenUrl is required"
        }
        val flowScopes = requireNotNull(scopes) {
            "ClientCredentials flow scopes is required"
        }
        return OAuthFlow.ClientCredentials(
            tokenUrl = flowTokenUrl,
            refreshUrl = refreshUrl,
            scopes = flowScopes,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [OAuthFlow.ClientCredentials] using DSL syntax.
 *
 * @param block configuration block for the client credentials flow
 * @return configured OAuthFlow.ClientCredentials object
 * @throws IllegalArgumentException if tokenUrl or scopes is not provided
 */
inline fun clientCredentialsFlow(block: ClientCredentialsFlowBuilder.() -> Unit): OAuthFlow.ClientCredentials {
    return ClientCredentialsFlowBuilder().apply(block).build()
}

/**
 * DSL builder for [OAuthFlow.AuthorizationCode].
 *
 * The builder enforces fail-fast validation:
 * - `authorizationUrl` is required
 * - `tokenUrl` is required
 * - `scopes` is required
 *
 * Example usage:
 * ```kotlin
 * val flow = authorizationCodeFlow {
 *     authorizationUrl = "https://example.com/oauth/authorize"
 *     tokenUrl = "https://example.com/oauth/token"
 *     refreshUrl = "https://example.com/oauth/refresh"
 *     scopes {
 *         "read:pets" description "Read your pets"
 *         "write:pets" description "Modify pets"
 *     }
 * }
 * ```
 */
class AuthorizationCodeFlowBuilder {
    var authorizationUrl: String? = null
    var tokenUrl: String? = null
    var refreshUrl: String? = null
    var scopes: Map<String, String>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures scopes using DSL syntax.
     */
    inline fun scopes(block: OAuthScopesBuilder.() -> Unit) {
        scopes = oauthScopes(block)
    }

    fun build(): OAuthFlow.AuthorizationCode {
        val flowAuthorizationUrl = requireNotNull(authorizationUrl) {
            "AuthorizationCode flow authorizationUrl is required"
        }
        val flowTokenUrl = requireNotNull(tokenUrl) {
            "AuthorizationCode flow tokenUrl is required"
        }
        val flowScopes = requireNotNull(scopes) {
            "AuthorizationCode flow scopes is required"
        }
        return OAuthFlow.AuthorizationCode(
            authorizationUrl = flowAuthorizationUrl,
            tokenUrl = flowTokenUrl,
            refreshUrl = refreshUrl,
            scopes = flowScopes,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [OAuthFlow.AuthorizationCode] using DSL syntax.
 *
 * @param block configuration block for the authorization code flow
 * @return configured OAuthFlow.AuthorizationCode object
 * @throws IllegalArgumentException if authorizationUrl, tokenUrl, or scopes is not provided
 */
inline fun authorizationCodeFlow(block: AuthorizationCodeFlowBuilder.() -> Unit): OAuthFlow.AuthorizationCode {
    return AuthorizationCodeFlowBuilder().apply(block).build()
}

/**
 * DSL builder for [OAuthFlow.DeviceAuthorization].
 *
 * The builder enforces fail-fast validation:
 * - `deviceAuthorizationUrl` is required
 * - `tokenUrl` is required
 * - `scopes` is required
 *
 * Example usage:
 * ```kotlin
 * val flow = deviceAuthorizationFlow {
 *     deviceAuthorizationUrl = "https://example.com/device/code"
 *     tokenUrl = "https://example.com/oauth/token"
 *     scopes {
 *         "read" description "Read access"
 *     }
 * }
 * ```
 */
class DeviceAuthorizationFlowBuilder {
    var deviceAuthorizationUrl: String? = null
    var tokenUrl: String? = null
    var refreshUrl: String? = null
    var scopes: Map<String, String>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures scopes using DSL syntax.
     */
    inline fun scopes(block: OAuthScopesBuilder.() -> Unit) {
        scopes = oauthScopes(block)
    }

    fun build(): OAuthFlow.DeviceAuthorization {
        val flowDeviceAuthorizationUrl = requireNotNull(deviceAuthorizationUrl) {
            "DeviceAuthorization flow deviceAuthorizationUrl is required"
        }
        val flowTokenUrl = requireNotNull(tokenUrl) {
            "DeviceAuthorization flow tokenUrl is required"
        }
        val flowScopes = requireNotNull(scopes) {
            "DeviceAuthorization flow scopes is required"
        }
        return OAuthFlow.DeviceAuthorization(
            deviceAuthorizationUrl = flowDeviceAuthorizationUrl,
            tokenUrl = flowTokenUrl,
            refreshUrl = refreshUrl,
            scopes = flowScopes,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [OAuthFlow.DeviceAuthorization] using DSL syntax.
 *
 * @param block configuration block for the device authorization flow
 * @return configured OAuthFlow.DeviceAuthorization object
 * @throws IllegalArgumentException if deviceAuthorizationUrl, tokenUrl, or scopes is not provided
 */
inline fun deviceAuthorizationFlow(block: DeviceAuthorizationFlowBuilder.() -> Unit): OAuthFlow.DeviceAuthorization {
    return DeviceAuthorizationFlowBuilder().apply(block).build()
}

// ============================================
// OAuthFlows DSL
// ============================================

/**
 * DSL builder for [OAuthFlows] object.
 *
 * Example usage:
 * ```kotlin
 * val flows = oauthFlows {
 *     implicit {
 *         authorizationUrl = "https://example.com/oauth/authorize"
 *         scopes {
 *             "read:pets" description "Read your pets"
 *         }
 *     }
 *     authorizationCode {
 *         authorizationUrl = "https://example.com/oauth/authorize"
 *         tokenUrl = "https://example.com/oauth/token"
 *         scopes {
 *             "read:pets" description "Read your pets"
 *             "write:pets" description "Modify pets"
 *         }
 *     }
 * }
 * ```
 */
class OAuthFlowsBuilder {
    var implicit: OAuthFlow.Implicit? = null
    var password: OAuthFlow.Password? = null
    var clientCredentials: OAuthFlow.ClientCredentials? = null
    var authorizationCode: OAuthFlow.AuthorizationCode? = null
    var deviceAuthorization: OAuthFlow.DeviceAuthorization? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the implicit flow using DSL syntax.
     */
    inline fun implicit(block: ImplicitFlowBuilder.() -> Unit) {
        implicit = implicitFlow(block)
    }

    /**
     * Configures the password flow using DSL syntax.
     */
    inline fun password(block: PasswordFlowBuilder.() -> Unit) {
        password = passwordFlow(block)
    }

    /**
     * Configures the client credentials flow using DSL syntax.
     */
    inline fun clientCredentials(block: ClientCredentialsFlowBuilder.() -> Unit) {
        clientCredentials = clientCredentialsFlow(block)
    }

    /**
     * Configures the authorization code flow using DSL syntax.
     */
    inline fun authorizationCode(block: AuthorizationCodeFlowBuilder.() -> Unit) {
        authorizationCode = authorizationCodeFlow(block)
    }

    /**
     * Configures the device authorization flow using DSL syntax.
     */
    inline fun deviceAuthorization(block: DeviceAuthorizationFlowBuilder.() -> Unit) {
        deviceAuthorization = deviceAuthorizationFlow(block)
    }

    fun build(): OAuthFlows = OAuthFlows(
        implicit = implicit,
        password = password,
        clientCredentials = clientCredentials,
        authorizationCode = authorizationCode,
        deviceAuthorization = deviceAuthorization,
        extensions = extensions,
    )
}

/**
 * Creates an [OAuthFlows] object using DSL syntax.
 *
 * @param block configuration block for the OAuth flows
 * @return configured OAuthFlows object
 */
inline fun oauthFlows(block: OAuthFlowsBuilder.() -> Unit): OAuthFlows {
    return OAuthFlowsBuilder().apply(block).build()
}

// ============================================
// SecurityScheme DSL Builders
// ============================================

/**
 * DSL builder for an API Key [SecurityScheme].
 *
 * The builder enforces fail-fast validation:
 * - `name` is required
 * - `location` is required
 *
 * Example usage:
 * ```kotlin
 * val scheme = apiKeyScheme {
 *     name = "X-API-Key"
 *     location = ApiKeyLocation.HEADER
 *     description = "API key for authentication"
 * }
 * ```
 */
class ApiKeySchemeBuilder {
    var name: String? = null
    var location: ApiKeyLocation? = null
    var description: String? = null
    var deprecated: Boolean = false
    var extensions: Map<String, JsonElement>? = null

    fun build(): SecurityScheme {
        val schemeName = requireNotNull(name) {
            "API Key security scheme name is required"
        }
        val schemeLocation = requireNotNull(location) {
            "API Key security scheme location is required"
        }
        return SecurityScheme.apiKey(
            name = schemeName,
            location = schemeLocation,
            description = description,
            deprecated = deprecated,
            extensions = extensions,
        )
    }
}

/**
 * Creates an API Key [SecurityScheme] using DSL syntax.
 *
 * @param block configuration block for the API key scheme
 * @return configured SecurityScheme object
 * @throws IllegalArgumentException if name or location is not provided
 */
inline fun apiKeyScheme(block: ApiKeySchemeBuilder.() -> Unit): SecurityScheme {
    return ApiKeySchemeBuilder().apply(block).build()
}

/**
 * DSL builder for an HTTP [SecurityScheme].
 *
 * The builder enforces fail-fast validation:
 * - `scheme` is required
 *
 * Example usage:
 * ```kotlin
 * val scheme = httpScheme {
 *     scheme = "bearer"
 *     bearerFormat = "JWT"
 *     description = "JWT Bearer token"
 * }
 * ```
 */
class HttpSchemeBuilder {
    var scheme: String? = null
    var bearerFormat: String? = null
    var description: String? = null
    var deprecated: Boolean = false
    var extensions: Map<String, JsonElement>? = null

    fun build(): SecurityScheme {
        val httpScheme = requireNotNull(scheme) {
            "HTTP security scheme 'scheme' is required"
        }
        return SecurityScheme.http(
            scheme = httpScheme,
            bearerFormat = bearerFormat,
            description = description,
            deprecated = deprecated,
            extensions = extensions,
        )
    }
}

/**
 * Creates an HTTP [SecurityScheme] using DSL syntax.
 *
 * @param block configuration block for the HTTP scheme
 * @return configured SecurityScheme object
 * @throws IllegalArgumentException if scheme is not provided
 */
inline fun httpScheme(block: HttpSchemeBuilder.() -> Unit): SecurityScheme {
    return HttpSchemeBuilder().apply(block).build()
}

/**
 * DSL builder for a Mutual TLS [SecurityScheme].
 *
 * Example usage:
 * ```kotlin
 * val scheme = mutualTLSScheme {
 *     description = "Mutual TLS authentication"
 * }
 * ```
 */
class MutualTLSSchemeBuilder {
    var description: String? = null
    var deprecated: Boolean = false
    var extensions: Map<String, JsonElement>? = null

    fun build(): SecurityScheme = SecurityScheme.mutualTLS(
        description = description,
        deprecated = deprecated,
        extensions = extensions,
    )
}

/**
 * Creates a Mutual TLS [SecurityScheme] using DSL syntax.
 *
 * @param block configuration block for the mutual TLS scheme
 * @return configured SecurityScheme object
 */
inline fun mutualTLSScheme(block: MutualTLSSchemeBuilder.() -> Unit): SecurityScheme {
    return MutualTLSSchemeBuilder().apply(block).build()
}

/**
 * DSL builder for an OAuth2 [SecurityScheme].
 *
 * The builder enforces fail-fast validation:
 * - `flows` is required
 *
 * Example usage:
 * ```kotlin
 * val scheme = oauth2Scheme {
 *     description = "OAuth2 authentication"
 *     flows {
 *         authorizationCode {
 *             authorizationUrl = "https://example.com/oauth/authorize"
 *             tokenUrl = "https://example.com/oauth/token"
 *             scopes {
 *                 "read:pets" description "Read your pets"
 *                 "write:pets" description "Modify pets"
 *             }
 *         }
 *     }
 * }
 * ```
 */
class OAuth2SchemeBuilder {
    var flows: OAuthFlows? = null
    var oauth2MetadataUrl: String? = null
    var description: String? = null
    var deprecated: Boolean = false
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures OAuth flows using DSL syntax.
     */
    inline fun flows(block: OAuthFlowsBuilder.() -> Unit) {
        flows = oauthFlows(block)
    }

    fun build(): SecurityScheme {
        val schemeFlows = requireNotNull(flows) {
            "OAuth2 security scheme flows is required"
        }
        return SecurityScheme.oauth2(
            flows = schemeFlows,
            oauth2MetadataUrl = oauth2MetadataUrl,
            description = description,
            deprecated = deprecated,
            extensions = extensions,
        )
    }
}

/**
 * Creates an OAuth2 [SecurityScheme] using DSL syntax.
 *
 * @param block configuration block for the OAuth2 scheme
 * @return configured SecurityScheme object
 * @throws IllegalArgumentException if flows is not provided
 */
inline fun oauth2Scheme(block: OAuth2SchemeBuilder.() -> Unit): SecurityScheme {
    return OAuth2SchemeBuilder().apply(block).build()
}

/**
 * DSL builder for an OpenID Connect [SecurityScheme].
 *
 * The builder enforces fail-fast validation:
 * - `openIdConnectUrl` is required
 *
 * Example usage:
 * ```kotlin
 * val scheme = openIdConnectScheme {
 *     openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
 *     description = "OpenID Connect discovery"
 * }
 * ```
 */
class OpenIdConnectSchemeBuilder {
    var openIdConnectUrl: String? = null
    var description: String? = null
    var deprecated: Boolean = false
    var extensions: Map<String, JsonElement>? = null

    fun build(): SecurityScheme {
        val schemeUrl = requireNotNull(openIdConnectUrl) {
            "OpenID Connect security scheme openIdConnectUrl is required"
        }
        return SecurityScheme.openIdConnect(
            openIdConnectUrl = schemeUrl,
            description = description,
            deprecated = deprecated,
            extensions = extensions,
        )
    }
}

/**
 * Creates an OpenID Connect [SecurityScheme] using DSL syntax.
 *
 * @param block configuration block for the OpenID Connect scheme
 * @return configured SecurityScheme object
 * @throws IllegalArgumentException if openIdConnectUrl is not provided
 */
inline fun openIdConnectScheme(block: OpenIdConnectSchemeBuilder.() -> Unit): SecurityScheme {
    return OpenIdConnectSchemeBuilder().apply(block).build()
}