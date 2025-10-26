package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SecuritySchemeTest {

    // API Key Security Scheme Tests

    @Test
    fun `should serialize ApiKey security scheme with query location`() {
        val securityScheme = SecurityScheme.apiKey(
            name = "api_key",
            location = ApiKeyLocation.QUERY
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"type\":\"apiKey\""))
        assert(json.contains("\"name\":\"api_key\""))
        assert(json.contains("\"in\":\"query\""))
    }

    @Test
    fun `should serialize ApiKey security scheme with header location`() {
        val securityScheme = SecurityScheme.apiKey(
            name = "X-API-Key",
            location = ApiKeyLocation.HEADER,
            description = "API key for authentication"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"name\":\"X-API-Key\""))
        assert(json.contains("\"in\":\"header\""))
        assert(json.contains("API key for authentication"))
    }

    @Test
    fun `should serialize ApiKey security scheme with cookie location`() {
        val securityScheme = SecurityScheme.apiKey(
            name = "session_id",
            location = ApiKeyLocation.COOKIE
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"in\":\"cookie\""))
    }

    @Test
    fun `should serialize ApiKey security scheme with deprecated flag`() {
        val securityScheme = SecurityScheme.apiKey(
            name = "old_key",
            location = ApiKeyLocation.HEADER,
            deprecated = true
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"deprecated\":true"))
    }

    // HTTP Security Scheme Tests

    @Test
    fun `should serialize HTTP security scheme with basic auth`() {
        val securityScheme = SecurityScheme.http(
            scheme = "basic",
            description = "Basic HTTP authentication"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"type\":\"http\""))
        assert(json.contains("\"scheme\":\"basic\""))
    }

    @Test
    fun `should serialize HTTP security scheme with bearer token`() {
        val securityScheme = SecurityScheme.http(
            scheme = "bearer",
            bearerFormat = "JWT",
            description = "Bearer token authentication"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"scheme\":\"bearer\""))
        assert(json.contains("\"bearerFormat\":\"JWT\""))
    }

    @Test
    fun `should serialize HTTP security scheme with digest auth`() {
        val securityScheme = SecurityScheme.http(
            scheme = "digest"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"scheme\":\"digest\""))
    }

    @Test
    fun `should serialize HTTP security scheme with custom scheme`() {
        val securityScheme = SecurityScheme.http(
            scheme = "negotiate",
            description = "Kerberos authentication"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"scheme\":\"negotiate\""))
    }

    // Mutual TLS Security Scheme Tests

    @Test
    fun `should serialize MutualTLS security scheme`() {
        val securityScheme = SecurityScheme.mutualTLS(
            description = "Mutual TLS authentication"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"type\":\"mutualTLS\""))
        assert(json.contains("Mutual TLS authentication"))
    }

    @Test
    fun `should serialize minimal MutualTLS security scheme`() {
        val securityScheme = SecurityScheme.mutualTLS()
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"type\":\"mutualTLS\""))
    }

    // OAuth2 Security Scheme Tests

    @Test
    fun `should serialize OAuth2 security scheme with implicit flow`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                implicit = OAuthFlow.Implicit(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    scopes = mapOf(
                        "read:pets" to "Read pets",
                        "write:pets" to "Write pets"
                    )
                )
            ),
            description = "OAuth2 Implicit flow"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"type\":\"oauth2\""))
        assert(json.contains("implicit"))
        assert(json.contains("https://example.com/oauth/authorize"))
        assert(json.contains("read:pets"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with password flow`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                password = OAuthFlow.Password(
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf(
                        "read" to "Read access",
                        "write" to "Write access"
                    )
                )
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("password"))
        assert(json.contains("https://example.com/oauth/token"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with client credentials flow`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                clientCredentials = OAuthFlow.ClientCredentials(
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf(
                        "api:read" to "Read API",
                        "api:write" to "Write API"
                    )
                )
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("clientCredentials"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with authorization code flow`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf(
                        "openid" to "OpenID Connect",
                        "profile" to "User profile",
                        "email" to "User email"
                    )
                )
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("authorizationCode"))
        assert(json.contains("authorizationUrl"))
        assert(json.contains("tokenUrl"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with device authorization flow`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                deviceAuthorization = OAuthFlow.DeviceAuthorization(
                    deviceAuthorizationUrl = "https://example.com/oauth/device",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf(
                        "device:read" to "Read from device",
                        "device:control" to "Control device"
                    )
                )
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("deviceAuthorization"))
        assert(json.contains("deviceAuthorizationUrl"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with multiple flows`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                implicit = OAuthFlow.Implicit(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    scopes = mapOf("read" to "Read access")
                ),
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf("read" to "Read access", "write" to "Write access")
                )
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("implicit"))
        assert(json.contains("authorizationCode"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with refresh URL`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    refreshUrl = "https://example.com/oauth/refresh",
                    scopes = mapOf("read" to "Read")
                )
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("refreshUrl"))
        assert(json.contains("https://example.com/oauth/refresh"))
    }

    @Test
    fun `should serialize OAuth2 security scheme with metadata URL`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = emptyMap()
                )
            ),
            oauth2MetadataUrl = "https://example.com/.well-known/oauth-authorization-server"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("oauth2MetadataUrl"))
        assert(json.contains(".well-known/oauth-authorization-server"))
    }

    // OpenID Connect Security Scheme Tests

    @Test
    fun `should serialize OpenIdConnect security scheme`() {
        val securityScheme = SecurityScheme.openIdConnect(
            openIdConnectUrl = "https://example.com/.well-known/openid-configuration",
            description = "OpenID Connect authentication"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("\"type\":\"openIdConnect\""))
        assert(json.contains("openIdConnectUrl"))
        assert(json.contains(".well-known/openid-configuration"))
    }

    @Test
    fun `should serialize OpenIdConnect security scheme without description`() {
        val securityScheme = SecurityScheme.openIdConnect(
            openIdConnectUrl = "https://accounts.google.com/.well-known/openid-configuration"
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("accounts.google.com"))
    }

    // Specification Extensions Tests

    @Test
    fun `should serialize security scheme with specification extensions`() {
        val securityScheme = SecurityScheme.apiKey(
            name = "api_key",
            location = ApiKeyLocation.HEADER,
            extensions = mapOf(
                "x-internal-id" to JsonPrimitive("sec-123"),
                "x-priority" to JsonPrimitive(1)
            )
        )
        val json = compactJson.encodeToString(securityScheme)

        assert(json.contains("x-internal-id"))
        assert(json.contains("sec-123"))
    }

    // Deserialization Tests

    @Test
    fun `should deserialize ApiKey security scheme`() {
        val json = """{"type":"apiKey","name":"api_key","in":"header"}"""
        val securityScheme = compactJson.decodeFromString<SecurityScheme>(json)

        assertEquals(SecuritySchemeType.API_KEY, securityScheme.type)
        assertEquals("api_key", securityScheme.name)
        assertEquals(ApiKeyLocation.HEADER, securityScheme.location)
    }

    @Test
    fun `should deserialize Http security scheme`() {
        val json = """{"type":"http","scheme":"bearer","bearerFormat":"JWT"}"""
        val securityScheme = compactJson.decodeFromString<SecurityScheme>(json)

        assertEquals(SecuritySchemeType.HTTP, securityScheme.type)
        assertEquals("bearer", securityScheme.scheme)
        assertEquals("JWT", securityScheme.bearerFormat)
    }

    @Test
    fun `should deserialize MutualTLS security scheme`() {
        val json = """{"type":"mutualTLS","description":"mTLS auth"}"""
        val securityScheme = compactJson.decodeFromString<SecurityScheme>(json)

        assertEquals(SecuritySchemeType.MUTUAL_TLS, securityScheme.type)
        assertEquals("mTLS auth", securityScheme.description)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip ApiKey security scheme`() {
        val securityScheme = SecurityScheme.apiKey(
            name = "X-API-KEY",
            location = ApiKeyLocation.HEADER,
            description = "API Key authentication"
        )

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), securityScheme)
    }

    @Test
    fun `should round-trip Http security scheme`() {
        val securityScheme = SecurityScheme.http(
            scheme = "bearer",
            bearerFormat = "JWT",
            description = "JWT Bearer token"
        )

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), securityScheme)
    }

    @Test
    fun `should round-trip OAuth2 security scheme`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    refreshUrl = "https://example.com/oauth/refresh",
                    scopes = mapOf(
                        "read:users" to "Read user data",
                        "write:users" to "Write user data"
                    )
                )
            ),
            description = "OAuth 2.0 Authorization Code flow"
        )

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), securityScheme)
    }

    @Test
    fun `should round-trip OpenIdConnect security scheme`() {
        val securityScheme = SecurityScheme.openIdConnect(
            openIdConnectUrl = "https://example.com/.well-known/openid-configuration",
            description = "OpenID Connect"
        )

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), securityScheme)
    }

    // Real-world Examples

    @Test
    fun `should serialize GitHub OAuth2 example`() {
        val securityScheme = SecurityScheme.oauth2(
            flows = OAuthFlows(
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://github.com/login/oauth/authorize",
                    tokenUrl = "https://github.com/login/oauth/access_token",
                    scopes = mapOf(
                        "repo" to "Full control of private repositories",
                        "user" to "Read and write access to user profile",
                        "read:org" to "Read org and team membership"
                    )
                )
            ),
            description = "GitHub OAuth2"
        )

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), securityScheme)
    }

    @Test
    fun `should serialize Google OpenID Connect example`() {
        val securityScheme = SecurityScheme.openIdConnect(
            openIdConnectUrl = "https://accounts.google.com/.well-known/openid-configuration",
            description = "Google Sign-In"
        )

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), securityScheme)
    }

    // Security Requirement Tests

    @Test
    fun `should serialize SecurityRequirement with OAuth2 scopes`() {
        val requirement = securityRequirement(
            "petstore_auth" to listOf("write:pets", "read:pets")
        )
        val json = compactJson.encodeToString(requirement)

        assert(json.contains("petstore_auth"))
        assert(json.contains("write:pets"))
        assert(json.contains("read:pets"))
    }

    @Test
    fun `should serialize SecurityRequirement with API key`() {
        val requirement = securityRequirement(
            "api_key" to emptyList()
        )
        val json = compactJson.encodeToString(requirement)

        assert(json.contains("api_key"))
        assertEquals("""{"api_key":[]}""", json)
    }

    @Test
    fun `should serialize SecurityRequirement with multiple schemes`() {
        val requirement = securityRequirement(
            "oauth2" to listOf("read", "write"),
            "api_key" to emptyList()
        )
        val json = compactJson.encodeToString(requirement)

        assert(json.contains("oauth2"))
        assert(json.contains("api_key"))
    }

    @Test
    fun `should deserialize SecurityRequirement`() {
        val json = """{"petstore_auth":["write:pets","read:pets"],"api_key":[]}"""
        val requirement = compactJson.decodeFromString<SecurityRequirement>(json)

        assertEquals(2, requirement.size)
        assertEquals(listOf("write:pets", "read:pets"), requirement["petstore_auth"])
        assertEquals(emptyList<String>(), requirement["api_key"])
    }

    @Test
    fun `should round-trip SecurityRequirement`() {
        val requirement = securityRequirement(
            "oauth2_auth" to listOf("openid", "profile", "email"),
            "api_key" to emptyList()
        )

        val json = compactJson.encodeToString(requirement)
        val deserialized = compactJson.decodeFromString<SecurityRequirement>(json)

        assertEquals(requirement, deserialized)
    }

    @Test
    fun `should serialize list of SecurityRequirements`() {
        val requirements = listOf(
            securityRequirement("oauth2" to listOf("read", "write")),
            securityRequirement("api_key" to emptyList())
        )
        val json = compactJson.encodeToString(requirements)

        assert(json.contains("oauth2"))
        assert(json.contains("api_key"))
    }
}
