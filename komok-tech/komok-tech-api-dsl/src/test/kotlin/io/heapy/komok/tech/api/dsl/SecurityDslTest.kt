package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SecurityDslTest {

    // ============================================
    // OAuthScopes DSL Tests
    // ============================================

    @Test
    fun `oauthScopes DSL should create empty scopes map`() {
        val result = oauthScopes {}

        assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun `oauthScopes DSL should create scopes with descriptions`() {
        val result = oauthScopes {
            "read:pets" description "Read your pets"
            "write:pets" description "Modify pets in your account"
        }

        assertEquals(
            mapOf(
                "read:pets" to "Read your pets",
                "write:pets" to "Modify pets in your account"
            ),
            result
        )
    }

    // ============================================
    // Implicit Flow DSL Tests
    // ============================================

    @Test
    fun `implicitFlow DSL should create flow with required fields`() {
        val result = implicitFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            scopes = mapOf("read:pets" to "Read your pets")
        }

        assertEquals(
            OAuthFlow.Implicit(
                authorizationUrl = "https://example.com/oauth/authorize",
                scopes = mapOf("read:pets" to "Read your pets")
            ),
            result
        )
    }

    @Test
    fun `implicitFlow DSL should create flow with all fields`() {
        val result = implicitFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read:pets" description "Read your pets"
                "write:pets" description "Modify pets"
            }
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertEquals(
            OAuthFlow.Implicit(
                authorizationUrl = "https://example.com/oauth/authorize",
                refreshUrl = "https://example.com/oauth/refresh",
                scopes = mapOf(
                    "read:pets" to "Read your pets",
                    "write:pets" to "Modify pets"
                ),
                extensions = mapOf("x-custom" to JsonPrimitive("value"))
            ),
            result
        )
    }

    @Test
    fun `implicitFlow DSL should fail when authorizationUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            implicitFlow {
                scopes = mapOf("read" to "Read access")
            }
        }

        assertEquals("Implicit flow authorizationUrl is required", exception.message)
    }

    @Test
    fun `implicitFlow DSL should fail when scopes is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            implicitFlow {
                authorizationUrl = "https://example.com/oauth/authorize"
            }
        }

        assertEquals("Implicit flow scopes is required", exception.message)
    }

    @Test
    fun `implicitFlow DSL should serialize correctly`() {
        val result = implicitFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            scopes {
                "read:pets" description "Read your pets"
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"authorizationUrl":"https://example.com/oauth/authorize","scopes":{"read:pets":"Read your pets"}}""",
            json
        )
    }

    @Test
    fun `implicitFlow DSL should round-trip correctly`() {
        val result = implicitFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read:pets" description "Read your pets"
                "write:pets" description "Modify pets"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OAuthFlow.Implicit.serializer(), result)
    }

    // ============================================
    // Password Flow DSL Tests
    // ============================================

    @Test
    fun `passwordFlow DSL should create flow with required fields`() {
        val result = passwordFlow {
            tokenUrl = "https://example.com/oauth/token"
            scopes = mapOf("read:users" to "Read user data")
        }

        assertEquals(
            OAuthFlow.Password(
                tokenUrl = "https://example.com/oauth/token",
                scopes = mapOf("read:users" to "Read user data")
            ),
            result
        )
    }

    @Test
    fun `passwordFlow DSL should create flow with all fields`() {
        val result = passwordFlow {
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read:users" description "Read user data"
                "write:users" description "Modify user data"
            }
            extensions = mapOf("x-rate-limit" to JsonPrimitive(100))
        }

        assertEquals(
            OAuthFlow.Password(
                tokenUrl = "https://example.com/oauth/token",
                refreshUrl = "https://example.com/oauth/refresh",
                scopes = mapOf(
                    "read:users" to "Read user data",
                    "write:users" to "Modify user data"
                ),
                extensions = mapOf("x-rate-limit" to JsonPrimitive(100))
            ),
            result
        )
    }

    @Test
    fun `passwordFlow DSL should fail when tokenUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            passwordFlow {
                scopes = mapOf("read" to "Read access")
            }
        }

        assertEquals("Password flow tokenUrl is required", exception.message)
    }

    @Test
    fun `passwordFlow DSL should fail when scopes is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            passwordFlow {
                tokenUrl = "https://example.com/oauth/token"
            }
        }

        assertEquals("Password flow scopes is required", exception.message)
    }

    @Test
    fun `passwordFlow DSL should serialize correctly`() {
        val result = passwordFlow {
            tokenUrl = "https://example.com/oauth/token"
            scopes {
                "read:users" description "Read user data"
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"tokenUrl":"https://example.com/oauth/token","scopes":{"read:users":"Read user data"}}""",
            json
        )
    }

    @Test
    fun `passwordFlow DSL should round-trip correctly`() {
        val result = passwordFlow {
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read:users" description "Read user data"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OAuthFlow.Password.serializer(), result)
    }

    // ============================================
    // ClientCredentials Flow DSL Tests
    // ============================================

    @Test
    fun `clientCredentialsFlow DSL should create flow with required fields`() {
        val result = clientCredentialsFlow {
            tokenUrl = "https://example.com/oauth/token"
            scopes = mapOf("admin" to "Full admin access")
        }

        assertEquals(
            OAuthFlow.ClientCredentials(
                tokenUrl = "https://example.com/oauth/token",
                scopes = mapOf("admin" to "Full admin access")
            ),
            result
        )
    }

    @Test
    fun `clientCredentialsFlow DSL should create flow with all fields`() {
        val result = clientCredentialsFlow {
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "admin" description "Full admin access"
                "read" description "Read-only access"
            }
            extensions = mapOf("x-service" to JsonPrimitive("backend"))
        }

        assertEquals(
            OAuthFlow.ClientCredentials(
                tokenUrl = "https://example.com/oauth/token",
                refreshUrl = "https://example.com/oauth/refresh",
                scopes = mapOf(
                    "admin" to "Full admin access",
                    "read" to "Read-only access"
                ),
                extensions = mapOf("x-service" to JsonPrimitive("backend"))
            ),
            result
        )
    }

    @Test
    fun `clientCredentialsFlow DSL should fail when tokenUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientCredentialsFlow {
                scopes = mapOf("admin" to "Admin access")
            }
        }

        assertEquals("ClientCredentials flow tokenUrl is required", exception.message)
    }

    @Test
    fun `clientCredentialsFlow DSL should fail when scopes is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientCredentialsFlow {
                tokenUrl = "https://example.com/oauth/token"
            }
        }

        assertEquals("ClientCredentials flow scopes is required", exception.message)
    }

    @Test
    fun `clientCredentialsFlow DSL should serialize correctly`() {
        val result = clientCredentialsFlow {
            tokenUrl = "https://example.com/oauth/token"
            scopes {
                "admin" description "Full admin access"
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"tokenUrl":"https://example.com/oauth/token","scopes":{"admin":"Full admin access"}}""",
            json
        )
    }

    @Test
    fun `clientCredentialsFlow DSL should round-trip correctly`() {
        val result = clientCredentialsFlow {
            tokenUrl = "https://example.com/oauth/token"
            scopes {
                "admin" description "Full admin access"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OAuthFlow.ClientCredentials.serializer(), result)
    }

    // ============================================
    // AuthorizationCode Flow DSL Tests
    // ============================================

    @Test
    fun `authorizationCodeFlow DSL should create flow with required fields`() {
        val result = authorizationCodeFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            tokenUrl = "https://example.com/oauth/token"
            scopes = mapOf("read:pets" to "Read your pets")
        }

        assertEquals(
            OAuthFlow.AuthorizationCode(
                authorizationUrl = "https://example.com/oauth/authorize",
                tokenUrl = "https://example.com/oauth/token",
                scopes = mapOf("read:pets" to "Read your pets")
            ),
            result
        )
    }

    @Test
    fun `authorizationCodeFlow DSL should create flow with all fields`() {
        val result = authorizationCodeFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read:pets" description "Read your pets"
                "write:pets" description "Modify pets"
            }
            extensions = mapOf("x-pkce" to JsonPrimitive(true))
        }

        assertEquals(
            OAuthFlow.AuthorizationCode(
                authorizationUrl = "https://example.com/oauth/authorize",
                tokenUrl = "https://example.com/oauth/token",
                refreshUrl = "https://example.com/oauth/refresh",
                scopes = mapOf(
                    "read:pets" to "Read your pets",
                    "write:pets" to "Modify pets"
                ),
                extensions = mapOf("x-pkce" to JsonPrimitive(true))
            ),
            result
        )
    }

    @Test
    fun `authorizationCodeFlow DSL should fail when authorizationUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authorizationCodeFlow {
                tokenUrl = "https://example.com/oauth/token"
                scopes = mapOf("read" to "Read access")
            }
        }

        assertEquals("AuthorizationCode flow authorizationUrl is required", exception.message)
    }

    @Test
    fun `authorizationCodeFlow DSL should fail when tokenUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authorizationCodeFlow {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes = mapOf("read" to "Read access")
            }
        }

        assertEquals("AuthorizationCode flow tokenUrl is required", exception.message)
    }

    @Test
    fun `authorizationCodeFlow DSL should fail when scopes is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authorizationCodeFlow {
                authorizationUrl = "https://example.com/oauth/authorize"
                tokenUrl = "https://example.com/oauth/token"
            }
        }

        assertEquals("AuthorizationCode flow scopes is required", exception.message)
    }

    @Test
    fun `authorizationCodeFlow DSL should serialize correctly`() {
        val result = authorizationCodeFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            tokenUrl = "https://example.com/oauth/token"
            scopes {
                "read:pets" description "Read your pets"
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"authorizationUrl":"https://example.com/oauth/authorize","tokenUrl":"https://example.com/oauth/token","scopes":{"read:pets":"Read your pets"}}""",
            json
        )
    }

    @Test
    fun `authorizationCodeFlow DSL should round-trip correctly`() {
        val result = authorizationCodeFlow {
            authorizationUrl = "https://example.com/oauth/authorize"
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read:pets" description "Read your pets"
                "write:pets" description "Modify pets"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OAuthFlow.AuthorizationCode.serializer(), result)
    }

    // ============================================
    // DeviceAuthorization Flow DSL Tests
    // ============================================

    @Test
    fun `deviceAuthorizationFlow DSL should create flow with required fields`() {
        val result = deviceAuthorizationFlow {
            deviceAuthorizationUrl = "https://example.com/device/code"
            tokenUrl = "https://example.com/oauth/token"
            scopes = mapOf("read" to "Read access")
        }

        assertEquals(
            OAuthFlow.DeviceAuthorization(
                deviceAuthorizationUrl = "https://example.com/device/code",
                tokenUrl = "https://example.com/oauth/token",
                scopes = mapOf("read" to "Read access")
            ),
            result
        )
    }

    @Test
    fun `deviceAuthorizationFlow DSL should create flow with all fields`() {
        val result = deviceAuthorizationFlow {
            deviceAuthorizationUrl = "https://example.com/device/code"
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read" description "Read access"
                "write" description "Write access"
            }
            extensions = mapOf("x-poll-interval" to JsonPrimitive(5))
        }

        assertEquals(
            OAuthFlow.DeviceAuthorization(
                deviceAuthorizationUrl = "https://example.com/device/code",
                tokenUrl = "https://example.com/oauth/token",
                refreshUrl = "https://example.com/oauth/refresh",
                scopes = mapOf(
                    "read" to "Read access",
                    "write" to "Write access"
                ),
                extensions = mapOf("x-poll-interval" to JsonPrimitive(5))
            ),
            result
        )
    }

    @Test
    fun `deviceAuthorizationFlow DSL should fail when deviceAuthorizationUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            deviceAuthorizationFlow {
                tokenUrl = "https://example.com/oauth/token"
                scopes = mapOf("read" to "Read access")
            }
        }

        assertEquals("DeviceAuthorization flow deviceAuthorizationUrl is required", exception.message)
    }

    @Test
    fun `deviceAuthorizationFlow DSL should fail when tokenUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            deviceAuthorizationFlow {
                deviceAuthorizationUrl = "https://example.com/device/code"
                scopes = mapOf("read" to "Read access")
            }
        }

        assertEquals("DeviceAuthorization flow tokenUrl is required", exception.message)
    }

    @Test
    fun `deviceAuthorizationFlow DSL should fail when scopes is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            deviceAuthorizationFlow {
                deviceAuthorizationUrl = "https://example.com/device/code"
                tokenUrl = "https://example.com/oauth/token"
            }
        }

        assertEquals("DeviceAuthorization flow scopes is required", exception.message)
    }

    @Test
    fun `deviceAuthorizationFlow DSL should serialize correctly`() {
        val result = deviceAuthorizationFlow {
            deviceAuthorizationUrl = "https://example.com/device/code"
            tokenUrl = "https://example.com/oauth/token"
            scopes {
                "read" description "Read access"
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"deviceAuthorizationUrl":"https://example.com/device/code","tokenUrl":"https://example.com/oauth/token","scopes":{"read":"Read access"}}""",
            json
        )
    }

    @Test
    fun `deviceAuthorizationFlow DSL should round-trip correctly`() {
        val result = deviceAuthorizationFlow {
            deviceAuthorizationUrl = "https://example.com/device/code"
            tokenUrl = "https://example.com/oauth/token"
            refreshUrl = "https://example.com/oauth/refresh"
            scopes {
                "read" description "Read access"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OAuthFlow.DeviceAuthorization.serializer(), result)
    }

    // ============================================
    // OAuthFlows DSL Tests
    // ============================================

    @Test
    fun `oauthFlows DSL should create empty flows`() {
        val result = oauthFlows {}

        assertEquals(OAuthFlows(), result)
    }

    @Test
    fun `oauthFlows DSL should create flows with implicit only`() {
        val result = oauthFlows {
            implicit {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes {
                    "read:pets" description "Read your pets"
                }
            }
        }

        assertEquals(
            OAuthFlows(
                implicit = OAuthFlow.Implicit(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    scopes = mapOf("read:pets" to "Read your pets")
                )
            ),
            result
        )
    }

    @Test
    fun `oauthFlows DSL should create flows with multiple flow types`() {
        val result = oauthFlows {
            implicit {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes {
                    "read:pets" description "Read your pets"
                }
            }
            authorizationCode {
                authorizationUrl = "https://example.com/oauth/authorize"
                tokenUrl = "https://example.com/oauth/token"
                scopes {
                    "read:pets" description "Read your pets"
                    "write:pets" description "Modify pets"
                }
            }
            clientCredentials {
                tokenUrl = "https://example.com/oauth/token"
                scopes {
                    "admin" description "Full admin access"
                }
            }
        }

        assertEquals(
            OAuthFlows(
                implicit = OAuthFlow.Implicit(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    scopes = mapOf("read:pets" to "Read your pets")
                ),
                clientCredentials = OAuthFlow.ClientCredentials(
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf("admin" to "Full admin access")
                ),
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf(
                        "read:pets" to "Read your pets",
                        "write:pets" to "Modify pets"
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `oauthFlows DSL should create flows with all flow types`() {
        val result = oauthFlows {
            implicit {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes { "read" description "Read access" }
            }
            password {
                tokenUrl = "https://example.com/oauth/token"
                scopes { "read" description "Read access" }
            }
            clientCredentials {
                tokenUrl = "https://example.com/oauth/token"
                scopes { "admin" description "Admin access" }
            }
            authorizationCode {
                authorizationUrl = "https://example.com/oauth/authorize"
                tokenUrl = "https://example.com/oauth/token"
                scopes { "read" description "Read access" }
            }
            deviceAuthorization {
                deviceAuthorizationUrl = "https://example.com/device/code"
                tokenUrl = "https://example.com/oauth/token"
                scopes { "read" description "Read access" }
            }
        }

        assertEquals(
            OAuthFlows(
                implicit = OAuthFlow.Implicit(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    scopes = mapOf("read" to "Read access")
                ),
                password = OAuthFlow.Password(
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf("read" to "Read access")
                ),
                clientCredentials = OAuthFlow.ClientCredentials(
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf("admin" to "Admin access")
                ),
                authorizationCode = OAuthFlow.AuthorizationCode(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf("read" to "Read access")
                ),
                deviceAuthorization = OAuthFlow.DeviceAuthorization(
                    deviceAuthorizationUrl = "https://example.com/device/code",
                    tokenUrl = "https://example.com/oauth/token",
                    scopes = mapOf("read" to "Read access")
                )
            ),
            result
        )
    }

    @Test
    fun `oauthFlows DSL should create flows with extensions`() {
        val result = oauthFlows {
            implicit {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes { "read" description "Read access" }
            }
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertEquals(
            OAuthFlows(
                implicit = OAuthFlow.Implicit(
                    authorizationUrl = "https://example.com/oauth/authorize",
                    scopes = mapOf("read" to "Read access")
                ),
                extensions = mapOf("x-custom" to JsonPrimitive("value"))
            ),
            result
        )
    }

    @Test
    fun `oauthFlows DSL should serialize correctly`() {
        val result = oauthFlows {
            implicit {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes {
                    "read:pets" description "Read your pets"
                }
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"implicit":{"authorizationUrl":"https://example.com/oauth/authorize","scopes":{"read:pets":"Read your pets"}}}""",
            json
        )
    }

    @Test
    fun `oauthFlows DSL should round-trip correctly`() {
        val result = oauthFlows {
            implicit {
                authorizationUrl = "https://example.com/oauth/authorize"
                scopes { "read" description "Read access" }
            }
            authorizationCode {
                authorizationUrl = "https://example.com/oauth/authorize"
                tokenUrl = "https://example.com/oauth/token"
                scopes { "read" description "Read access" }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(OAuthFlows.serializer(), result)
    }

    // ============================================
    // API Key SecurityScheme DSL Tests
    // ============================================

    @Test
    fun `apiKeyScheme DSL should create scheme with required fields`() {
        val result = apiKeyScheme {
            name = "X-API-Key"
            location = ApiKeyLocation.HEADER
        }

        assertEquals(
            SecurityScheme.apiKey(
                name = "X-API-Key",
                location = ApiKeyLocation.HEADER
            ),
            result
        )
    }

    @Test
    fun `apiKeyScheme DSL should create scheme with all fields`() {
        val result = apiKeyScheme {
            name = "api_key"
            location = ApiKeyLocation.QUERY
            description = "API key for authentication"
            deprecated = true
            extensions = mapOf("x-custom" to JsonPrimitive("value"))
        }

        assertEquals(
            SecurityScheme.apiKey(
                name = "api_key",
                location = ApiKeyLocation.QUERY,
                description = "API key for authentication",
                deprecated = true,
                extensions = mapOf("x-custom" to JsonPrimitive("value"))
            ),
            result
        )
    }

    @Test
    fun `apiKeyScheme DSL should create scheme with cookie location`() {
        val result = apiKeyScheme {
            name = "session_id"
            location = ApiKeyLocation.COOKIE
        }

        assertEquals(
            SecurityScheme.apiKey(
                name = "session_id",
                location = ApiKeyLocation.COOKIE
            ),
            result
        )
    }

    @Test
    fun `apiKeyScheme DSL should fail when name is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            apiKeyScheme {
                location = ApiKeyLocation.HEADER
            }
        }

        assertEquals("API Key security scheme name is required", exception.message)
    }

    @Test
    fun `apiKeyScheme DSL should fail when location is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            apiKeyScheme {
                name = "X-API-Key"
            }
        }

        assertEquals("API Key security scheme location is required", exception.message)
    }

    @Test
    fun `apiKeyScheme DSL should serialize correctly`() {
        val result = apiKeyScheme {
            name = "X-API-Key"
            location = ApiKeyLocation.HEADER
            description = "API key auth"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"apiKey","description":"API key auth","name":"X-API-Key","in":"header"}""",
            json
        )
    }

    @Test
    fun `apiKeyScheme DSL should round-trip correctly`() {
        val result = apiKeyScheme {
            name = "X-API-Key"
            location = ApiKeyLocation.HEADER
            description = "API key authentication"
        }

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), result)
    }

    // ============================================
    // HTTP SecurityScheme DSL Tests
    // ============================================

    @Test
    fun `httpScheme DSL should create scheme with required fields`() {
        val result = httpScheme {
            scheme = "bearer"
        }

        assertEquals(
            SecurityScheme.http(scheme = "bearer"),
            result
        )
    }

    @Test
    fun `httpScheme DSL should create scheme with all fields`() {
        val result = httpScheme {
            scheme = "bearer"
            bearerFormat = "JWT"
            description = "JWT Bearer token authentication"
            deprecated = true
            extensions = mapOf("x-token-ttl" to JsonPrimitive(3600))
        }

        assertEquals(
            SecurityScheme.http(
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "JWT Bearer token authentication",
                deprecated = true,
                extensions = mapOf("x-token-ttl" to JsonPrimitive(3600))
            ),
            result
        )
    }

    @Test
    fun `httpScheme DSL should create basic auth scheme`() {
        val result = httpScheme {
            scheme = "basic"
            description = "Basic HTTP authentication"
        }

        assertEquals(
            SecurityScheme.http(
                scheme = "basic",
                description = "Basic HTTP authentication"
            ),
            result
        )
    }

    @Test
    fun `httpScheme DSL should fail when scheme is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            httpScheme {
                bearerFormat = "JWT"
            }
        }

        assertEquals("HTTP security scheme 'scheme' is required", exception.message)
    }

    @Test
    fun `httpScheme DSL should serialize correctly`() {
        val result = httpScheme {
            scheme = "bearer"
            bearerFormat = "JWT"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"http","scheme":"bearer","bearerFormat":"JWT"}""",
            json
        )
    }

    @Test
    fun `httpScheme DSL should round-trip correctly`() {
        val result = httpScheme {
            scheme = "bearer"
            bearerFormat = "JWT"
            description = "JWT Bearer token"
        }

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), result)
    }

    // ============================================
    // Mutual TLS SecurityScheme DSL Tests
    // ============================================

    @Test
    fun `mutualTLSScheme DSL should create scheme with no fields`() {
        val result = mutualTLSScheme {}

        assertEquals(
            SecurityScheme.mutualTLS(),
            result
        )
    }

    @Test
    fun `mutualTLSScheme DSL should create scheme with all fields`() {
        val result = mutualTLSScheme {
            description = "Mutual TLS client certificate authentication"
            deprecated = true
            extensions = mapOf("x-cert-required" to JsonPrimitive(true))
        }

        assertEquals(
            SecurityScheme.mutualTLS(
                description = "Mutual TLS client certificate authentication",
                deprecated = true,
                extensions = mapOf("x-cert-required" to JsonPrimitive(true))
            ),
            result
        )
    }

    @Test
    fun `mutualTLSScheme DSL should serialize correctly`() {
        val result = mutualTLSScheme {
            description = "mTLS auth"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"mutualTLS","description":"mTLS auth"}""",
            json
        )
    }

    @Test
    fun `mutualTLSScheme DSL should round-trip correctly`() {
        val result = mutualTLSScheme {
            description = "Mutual TLS authentication"
        }

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), result)
    }

    // ============================================
    // OAuth2 SecurityScheme DSL Tests
    // ============================================

    @Test
    fun `oauth2Scheme DSL should create scheme with implicit flow`() {
        val result = oauth2Scheme {
            flows {
                implicit {
                    authorizationUrl = "https://example.com/oauth/authorize"
                    scopes {
                        "read:pets" description "Read your pets"
                    }
                }
            }
        }

        assertEquals(
            SecurityScheme.oauth2(
                flows = OAuthFlows(
                    implicit = OAuthFlow.Implicit(
                        authorizationUrl = "https://example.com/oauth/authorize",
                        scopes = mapOf("read:pets" to "Read your pets")
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `oauth2Scheme DSL should create scheme with all fields`() {
        val result = oauth2Scheme {
            description = "OAuth2 authentication"
            oauth2MetadataUrl = "https://example.com/.well-known/oauth-authorization-server"
            deprecated = true
            flows {
                authorizationCode {
                    authorizationUrl = "https://example.com/oauth/authorize"
                    tokenUrl = "https://example.com/oauth/token"
                    refreshUrl = "https://example.com/oauth/refresh"
                    scopes {
                        "read:pets" description "Read your pets"
                        "write:pets" description "Modify pets"
                    }
                }
            }
            extensions = mapOf("x-oauth-provider" to JsonPrimitive("custom"))
        }

        assertEquals(
            SecurityScheme.oauth2(
                flows = OAuthFlows(
                    authorizationCode = OAuthFlow.AuthorizationCode(
                        authorizationUrl = "https://example.com/oauth/authorize",
                        tokenUrl = "https://example.com/oauth/token",
                        refreshUrl = "https://example.com/oauth/refresh",
                        scopes = mapOf(
                            "read:pets" to "Read your pets",
                            "write:pets" to "Modify pets"
                        )
                    )
                ),
                oauth2MetadataUrl = "https://example.com/.well-known/oauth-authorization-server",
                description = "OAuth2 authentication",
                deprecated = true,
                extensions = mapOf("x-oauth-provider" to JsonPrimitive("custom"))
            ),
            result
        )
    }

    @Test
    fun `oauth2Scheme DSL should create scheme with multiple flows`() {
        val result = oauth2Scheme {
            flows {
                implicit {
                    authorizationUrl = "https://example.com/oauth/authorize"
                    scopes { "read" description "Read access" }
                }
                clientCredentials {
                    tokenUrl = "https://example.com/oauth/token"
                    scopes { "admin" description "Admin access" }
                }
            }
        }

        assertEquals(
            SecurityScheme.oauth2(
                flows = OAuthFlows(
                    implicit = OAuthFlow.Implicit(
                        authorizationUrl = "https://example.com/oauth/authorize",
                        scopes = mapOf("read" to "Read access")
                    ),
                    clientCredentials = OAuthFlow.ClientCredentials(
                        tokenUrl = "https://example.com/oauth/token",
                        scopes = mapOf("admin" to "Admin access")
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `oauth2Scheme DSL should fail when flows is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            oauth2Scheme {
                description = "Missing flows"
            }
        }

        assertEquals("OAuth2 security scheme flows is required", exception.message)
    }

    @Test
    fun `oauth2Scheme DSL should serialize correctly`() {
        val result = oauth2Scheme {
            flows {
                implicit {
                    authorizationUrl = "https://example.com/oauth/authorize"
                    scopes {
                        "read:pets" description "Read your pets"
                    }
                }
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"oauth2","flows":{"implicit":{"authorizationUrl":"https://example.com/oauth/authorize","scopes":{"read:pets":"Read your pets"}}}}""",
            json
        )
    }

    @Test
    fun `oauth2Scheme DSL should round-trip correctly`() {
        val result = oauth2Scheme {
            description = "OAuth2 auth"
            flows {
                authorizationCode {
                    authorizationUrl = "https://example.com/oauth/authorize"
                    tokenUrl = "https://example.com/oauth/token"
                    scopes {
                        "read:pets" description "Read your pets"
                        "write:pets" description "Modify pets"
                    }
                }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), result)
    }

    // ============================================
    // OpenID Connect SecurityScheme DSL Tests
    // ============================================

    @Test
    fun `openIdConnectScheme DSL should create scheme with required fields`() {
        val result = openIdConnectScheme {
            openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
        }

        assertEquals(
            SecurityScheme.openIdConnect(
                openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
            ),
            result
        )
    }

    @Test
    fun `openIdConnectScheme DSL should create scheme with all fields`() {
        val result = openIdConnectScheme {
            openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
            description = "OpenID Connect Discovery"
            deprecated = true
            extensions = mapOf("x-provider" to JsonPrimitive("keycloak"))
        }

        assertEquals(
            SecurityScheme.openIdConnect(
                openIdConnectUrl = "https://example.com/.well-known/openid-configuration",
                description = "OpenID Connect Discovery",
                deprecated = true,
                extensions = mapOf("x-provider" to JsonPrimitive("keycloak"))
            ),
            result
        )
    }

    @Test
    fun `openIdConnectScheme DSL should fail when openIdConnectUrl is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            openIdConnectScheme {
                description = "Missing URL"
            }
        }

        assertEquals("OpenID Connect security scheme openIdConnectUrl is required", exception.message)
    }

    @Test
    fun `openIdConnectScheme DSL should serialize correctly`() {
        val result = openIdConnectScheme {
            openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"type":"openIdConnect","openIdConnectUrl":"https://example.com/.well-known/openid-configuration"}""",
            json
        )
    }

    @Test
    fun `openIdConnectScheme DSL should round-trip correctly`() {
        val result = openIdConnectScheme {
            openIdConnectUrl = "https://example.com/.well-known/openid-configuration"
            description = "OIDC authentication"
        }

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), result)
    }

    // ============================================
    // SecurityRequirement DSL Tests
    // (SecurityRequirementsBuilder already exists in OperationAndPathDsl)
    // ============================================

    @Test
    fun `securityRequirements DSL should create empty list`() {
        val result = securityRequirements {}

        assertEquals(emptyList<SecurityRequirement>(), result)
    }

    @Test
    fun `securityRequirements DSL should create single requirement without scopes`() {
        val result = securityRequirements {
            requirement("api_key")
        }

        assertEquals(
            listOf(mapOf("api_key" to emptyList<String>())),
            result
        )
    }

    @Test
    fun `securityRequirements DSL should create single requirement with scopes`() {
        val result = securityRequirements {
            requirement("petstore_auth", "read:pets", "write:pets")
        }

        assertEquals(
            listOf(mapOf("petstore_auth" to listOf("read:pets", "write:pets"))),
            result
        )
    }

    @Test
    fun `securityRequirements DSL should create multiple requirements (OR logic)`() {
        val result = securityRequirements {
            requirement("api_key")
            requirement("petstore_auth", "read:pets", "write:pets")
        }

        assertEquals(
            listOf(
                mapOf("api_key" to emptyList<String>()),
                mapOf("petstore_auth" to listOf("read:pets", "write:pets"))
            ),
            result
        )
    }

    @Test
    fun `securityRequirements DSL should create combined requirement (AND logic)`() {
        val result = securityRequirements {
            requirements(
                "api_key" to emptyList<String>(),
                "petstore_auth" to listOf("read:pets")
            )
        }

        assertEquals(
            listOf(
                mapOf(
                    "api_key" to emptyList<String>(),
                    "petstore_auth" to listOf("read:pets")
                )
            ),
            result
        )
    }

    // ============================================
    // Integration Tests: Security DSLs with Operation
    // ============================================

    @Test
    fun `security DSLs should integrate with operation DSL`() {
        val result = operation {
            summary = "List pets"
            operationId = "listPets"
            security {
                requirement("petstore_auth", "read:pets")
            }
            responses {
                "200" to {
                    description = "A list of pets"
                }
            }
        }

        assertEquals("listPets", result.operationId)
        assertEquals(
            listOf(mapOf("petstore_auth" to listOf("read:pets"))),
            result.security
        )
    }

    @Test
    fun `oauth2Scheme DSL should work in a realistic Petstore scenario`() {
        val petstoreAuth = oauth2Scheme {
            description = "Petstore OAuth2 authentication"
            flows {
                implicit {
                    authorizationUrl = "https://petstore.swagger.io/oauth/authorize"
                    scopes {
                        "read:pets" description "read your pets"
                        "write:pets" description "modify pets in your account"
                    }
                }
            }
        }

        val apiKey = apiKeyScheme {
            name = "api_key"
            location = ApiKeyLocation.HEADER
        }

        assertEquals(SecuritySchemeType.OAUTH2, petstoreAuth.type)
        assertEquals("Petstore OAuth2 authentication", petstoreAuth.description)
        assertEquals(SecuritySchemeType.API_KEY, apiKey.type)
        assertEquals("api_key", apiKey.name)
        assertEquals(ApiKeyLocation.HEADER, apiKey.location)

        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), petstoreAuth)
        TestHelpers.testRoundTripWithoutValidation(SecurityScheme.serializer(), apiKey)
    }
}
