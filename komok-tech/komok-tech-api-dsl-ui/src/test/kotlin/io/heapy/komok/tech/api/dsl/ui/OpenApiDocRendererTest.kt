package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.*
import io.heapy.komok.tech.logging.Logger
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OpenApiDocRendererTest {

    private companion object : Logger()

    @Test
    fun `should generate HTML from minimal OpenAPI object`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify HTML structure
        assertTrue(html.contains("<html"), "HTML should have root element")
        assertTrue(html.contains("<head"), "HTML should have head section")
        assertTrue(html.contains("<body") || html.contains("api-doc-container"), "HTML should have body section")
        assertTrue(html.contains("Test API"), "HTML should contain API title")
        assertTrue(html.contains("1.0.0"), "HTML should contain API version")
    }

    @Test
    fun `should generate HTML with CSS styles embedded`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify CSS is embedded
        assertTrue(html.contains("<style>"), "HTML should have embedded styles")
        assertTrue(html.contains("--bg-primary"), "CSS should contain theme variables")
        assertTrue(html.contains(".api-doc-container"), "CSS should contain layout classes")
        assertTrue(html.contains(".method-badge"), "CSS should contain method badge styles")
    }

    @Test
    fun `should generate HTML with JavaScript embedded`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify JavaScript is embedded
        assertTrue(html.contains("<script"), "HTML should have embedded script")
        assertTrue(html.contains("initTheme"), "JavaScript should have theme initialization")
        assertTrue(html.contains("initSearch"), "JavaScript should have search functionality")
        assertTrue(html.contains("localStorage"), "JavaScript should use localStorage for theme persistence")
    }

    @Test
    fun `should render API information section`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Pet Store API",
                version = "2.0.0",
                summary = "A sample pet store API",
                description = "This API manages pets and stores",
                termsOfService = "https://example.com/terms",
                contact = Contact(
                    name = "API Support",
                    email = "support@example.com",
                    url = "https://example.com/support"
                ),
                license = License(
                    name = "MIT",
                    url = "https://opensource.org/licenses/MIT"
                )
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify info section
        assertTrue(html.contains("Pet Store API"), "Should contain API title")
        assertTrue(html.contains("2.0.0"), "Should contain version")
        assertTrue(html.contains("A sample pet store API"), "Should contain summary")
        assertTrue(html.contains("This API manages pets and stores"), "Should contain description")
        assertTrue(html.contains("https://example.com/terms"), "Should contain terms of service")
        assertTrue(html.contains("API Support"), "Should contain contact name")
        assertTrue(html.contains("support@example.com"), "Should contain contact email")
        assertTrue(html.contains("MIT"), "Should contain license name")
    }

    @Test
    fun `should render servers section`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            servers = listOf(
                Server(
                    url = "https://api.example.com/v1",
                    description = "Production server"
                ),
                Server(
                    url = "https://staging.example.com/v1",
                    description = "Staging server"
                )
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify servers section
        assertTrue(html.contains("Servers"), "Should have servers section")
        assertTrue(html.contains("https://api.example.com/v1"), "Should contain production server URL")
        assertTrue(html.contains("Production server"), "Should contain production server description")
        assertTrue(html.contains("https://staging.example.com/v1"), "Should contain staging server URL")
        assertTrue(html.contains("Staging server"), "Should contain staging server description")
    }

    @Test
    fun `should render endpoints with operations`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List users",
                        description = "Get a list of all users",
                        responses = mapOf(
                            "200" to Response(
                                summary = "Success",
                                description = "List of users returned successfully"
                            )
                        )
                    ),
                    post = Operation(
                        summary = "Create user",
                        description = "Create a new user",
                        responses = mapOf(
                            "201" to Response(
                                summary = "Created",
                                description = "User created successfully"
                            )
                        )
                    )
                ),
                "/users/{id}" to PathItem(
                    get = Operation(
                        summary = "Get user",
                        description = "Get a specific user by ID",
                        responses = mapOf(
                            "200" to Response(
                                summary = "Success",
                                description = "User found"
                            ),
                            "404" to Response(
                                summary = "Not Found",
                                description = "User not found"
                            )
                        )
                    ),
                    delete = Operation(
                        summary = "Delete user",
                        description = "Delete a user",
                        responses = mapOf(
                            "204" to Response(
                                summary = "No Content",
                                description = "User deleted"
                            )
                        )
                    )
                )
            )
        )

        val html = renderOpenApiDoc(openapi)

        // Verify endpoints
        assertTrue(html.contains("Endpoints"), "Should have endpoints section")
        assertTrue(html.contains("/users"), "Should contain /users path")
        assertTrue(html.contains("/users/{id}"), "Should contain /users/{id} path")

        // Verify HTTP methods
        assertTrue(html.contains("GET"), "Should contain GET method")
        assertTrue(html.contains("POST"), "Should contain POST method")
        assertTrue(html.contains("DELETE"), "Should contain DELETE method")

        // Verify operations
        assertTrue(html.contains("List users"), "Should contain GET operation summary")
        assertTrue(html.contains("Create user"), "Should contain POST operation summary")
        assertTrue(html.contains("Get user"), "Should contain GET user operation summary")
        assertTrue(html.contains("Delete user"), "Should contain DELETE operation summary")

        // Verify responses
        assertTrue(html.contains("200"), "Should contain 200 status code")
        assertTrue(html.contains("201"), "Should contain 201 status code")
        assertTrue(html.contains("404"), "Should contain 404 status code")
        assertTrue(html.contains("204"), "Should contain 204 status code")
    }

    @Test
    fun `should render operation with parameters`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        summary = "List users",
                        parameters = listOf(
                            Direct(
                                Parameter(
                                    name = "limit",
                                    location = ParameterLocation.QUERY,
                                    description = "Maximum number of users to return",
                                    required = false,
                                    schema = Schema(buildJsonObject { put("type", "integer") })
                                )
                            ),
                            Direct(
                                Parameter(
                                    name = "offset",
                                    location = ParameterLocation.QUERY,
                                    description = "Number of users to skip",
                                    required = false,
                                    schema = Schema(buildJsonObject { put("type", "integer") })
                                )
                            ),
                            Direct(
                                Parameter(
                                    name = "Authorization",
                                    location = ParameterLocation.HEADER,
                                    description = "Bearer token",
                                    required = true,
                                    schema = Schema(buildJsonObject { put("type", "string") })
                                ),
                            ),
                        ),
                        responses = mapOf(
                            "200" to Response(
                                summary = "Success",
                                description = "List of users"
                            )
                        )
                    )
                )
            )
        )

        val html = renderOpenApiDoc(openapi)

        // Verify parameters section
        assertTrue(html.contains("Parameters"), "Should have parameters section")
        assertTrue(html.contains("limit"), "Should contain limit parameter")
        assertTrue(html.contains("offset"), "Should contain offset parameter")
        assertTrue(html.contains("Authorization"), "Should contain Authorization parameter")
        assertTrue(html.contains("query"), "Should show query parameter location")
        assertTrue(html.contains("header"), "Should show header parameter location")
        assertTrue(html.contains("Maximum number of users to return"), "Should contain parameter description")
    }

    @Test
    fun `should generate valid HTML structure`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Test API",
                version = "1.0.0"
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Basic HTML structure validation
        val htmlTagCount = html.split("<html").size - 1
        val htmlCloseTagCount = html.split("</html>").size - 1
        assertEquals(1, htmlTagCount, "Should have exactly one <html> tag")
        assertEquals(1, htmlCloseTagCount, "Should have exactly one </html> tag")

        val headTagCount = html.split("<head>").size - 1
        val headCloseTagCount = html.split("</head>").size - 1
        assertEquals(1, headTagCount, "Should have exactly one <head> tag")
        assertEquals(1, headCloseTagCount, "Should have exactly one </head> tag")

        val bodyTagCount = html.split("<body").size - 1
        val bodyCloseTagCount = html.split("</body>").size - 1
        assertEquals(1, bodyTagCount, "Should have exactly one <body> tag")
        assertEquals(1, bodyCloseTagCount, "Should have exactly one </body> tag")

        println("Generated HTML length: ${html.length} characters")
    }

    @Test
    fun `should render security schemes for all types`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Security Test API",
                version = "1.0.0"
            ),
            components = Components(
                securitySchemes = mapOf(
                    "api_key" to SecurityScheme.apiKey(
                        name = "X-API-Key",
                        location = ApiKeyLocation.HEADER,
                        description = "API key authentication"
                    ),
                    "bearer_auth" to SecurityScheme.http(
                        scheme = "bearer",
                        bearerFormat = "JWT",
                        description = "JWT authentication"
                    ),
                    "mutual_tls" to SecurityScheme.mutualTLS(
                        description = "Mutual TLS authentication"
                    ),
                    "oauth2" to SecurityScheme.oauth2(
                        description = "OAuth 2.0 authentication",
                        flows = OAuthFlows(
                            authorizationCode = OAuthFlow.AuthorizationCode(
                                authorizationUrl = "https://auth.example.com/authorize",
                                tokenUrl = "https://auth.example.com/token",
                                scopes = mapOf(
                                    "read:users" to "Read user data",
                                    "write:users" to "Modify user data"
                                )
                            ),
                            implicit = OAuthFlow.Implicit(
                                authorizationUrl = "https://auth.example.com/authorize",
                                scopes = mapOf(
                                    "read:users" to "Read user data"
                                )
                            )
                        )
                    ),
                    "openid" to SecurityScheme.openIdConnect(
                        openIdConnectUrl = "https://auth.example.com/.well-known/openid-configuration",
                        description = "OpenID Connect authentication"
                    )
                )
            ),
            security = listOf(
                securityRequirement("bearer_auth" to emptyList()),
                securityRequirement("oauth2" to listOf("read:users"))
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify security section exists
        assertTrue(html.contains("id=\"security\""), "Should have security section with id")
        assertTrue(html.contains("Security"), "Should have security heading")

        // Verify API Key scheme
        assertTrue(html.contains("api_key"), "Should contain api_key scheme name")
        assertTrue(html.contains("API Key"), "Should contain API Key type badge")
        assertTrue(html.contains("X-API-Key"), "Should contain API key parameter name")
        assertTrue(html.contains("header"), "Should contain API key location")
        assertTrue(html.contains("API key authentication"), "Should contain api_key description")

        // Verify HTTP Bearer scheme
        assertTrue(html.contains("bearer_auth"), "Should contain bearer_auth scheme name")
        assertTrue(html.contains("HTTP"), "Should contain HTTP type badge")
        assertTrue(html.contains("bearer"), "Should contain bearer scheme")
        assertTrue(html.contains("JWT"), "Should contain JWT bearer format")
        assertTrue(html.contains("JWT authentication"), "Should contain bearer_auth description")

        // Verify Mutual TLS scheme
        assertTrue(html.contains("mutual_tls"), "Should contain mutual_tls scheme name")
        assertTrue(html.contains("Mutual TLS"), "Should contain Mutual TLS type badge")
        assertTrue(html.contains("Mutual TLS authentication"), "Should contain mutual_tls description")

        // Verify OAuth2 scheme
        assertTrue(html.contains("oauth2"), "Should contain oauth2 scheme name")
        assertTrue(html.contains("OAuth 2.0"), "Should contain OAuth 2.0 type badge")
        assertTrue(html.contains("Authorization Code"), "Should contain Authorization Code flow")
        assertTrue(html.contains("Implicit"), "Should contain Implicit flow")
        assertTrue(html.contains("https://auth.example.com/authorize"), "Should contain authorization URL")
        assertTrue(html.contains("https://auth.example.com/token"), "Should contain token URL")
        assertTrue(html.contains("read:users"), "Should contain scope name")
        assertTrue(html.contains("Read user data"), "Should contain scope description")
        assertTrue(html.contains("write:users"), "Should contain write scope")

        // Verify OpenID Connect scheme
        assertTrue(html.contains("openid"), "Should contain openid scheme name")
        assertTrue(html.contains("OpenID Connect"), "Should contain OpenID Connect type badge")
        assertTrue(
            html.contains("https://auth.example.com/.well-known/openid-configuration"),
            "Should contain discovery URL"
        )

        // Verify global security requirements
        assertTrue(html.contains("Global Security Requirements"), "Should show global security requirements")
        assertTrue(html.contains("bearer_auth"), "Should list bearer_auth in global requirements")
    }

    @Test
    fun `should render external documentation links`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "External Docs Test API",
                version = "1.0.0"
            ),
            externalDocs = ExternalDocumentation(
                url = "https://docs.example.com",
                description = "Full API documentation"
            ),
            tags = listOf(
                Tag(
                    name = "users",
                    description = "User management endpoints",
                    externalDocs = ExternalDocumentation(
                        url = "https://docs.example.com/users",
                        description = "User docs"
                    )
                )
            ),
            paths = mapOf(
                "/users" to PathItem(
                    get = Operation(
                        tags = listOf("users"),
                        summary = "List users",
                        externalDocs = ExternalDocumentation(
                            url = "https://docs.example.com/users/list",
                            description = "List users documentation"
                        ),
                        responses = mapOf(
                            "200" to Response(description = "Success")
                        )
                    )
                )
            )
        )

        val html = renderOpenApiDoc(openapi)

        // Verify root external docs
        assertTrue(html.contains("https://docs.example.com"), "Should contain root external docs URL")
        assertTrue(html.contains("Full API documentation"), "Should contain root external docs description")
        assertTrue(html.contains("external-docs-link"), "Should use external-docs-link CSS class")

        // Verify tag external docs
        assertTrue(html.contains("https://docs.example.com/users"), "Should contain tag external docs URL")
        assertTrue(html.contains("User docs"), "Should contain tag external docs description")

        // Verify operation external docs
        assertTrue(html.contains("https://docs.example.com/users/list"), "Should contain operation external docs URL")
        assertTrue(html.contains("List users documentation"), "Should contain operation external docs description")
    }

    @Test
    fun `should render collapsible operation HTML structure`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Collapsible Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/test" to PathItem(
                    get = Operation(
                        summary = "Test endpoint",
                        description = "A test endpoint with collapsible details",
                        parameters = listOf(
                            Direct(
                                Parameter(
                                    name = "q",
                                    location = ParameterLocation.QUERY,
                                    description = "Search query",
                                    schema = Schema(buildJsonObject { put("type", "string") })
                                )
                            )
                        ),
                        responses = mapOf(
                            "200" to Response(description = "Success")
                        )
                    )
                )
            )
        )

        val html = renderOpenApiDoc(openapi)

        // Verify collapsible structure
        assertTrue(html.contains("operation-toggle"), "Should have operation toggle button")
        assertTrue(html.contains("operation-toggle-icon"), "Should have toggle icon")
        assertTrue(html.contains("operation-details"), "Should have operation-details wrapper")
        assertTrue(html.contains("initOperationCollapse"), "JavaScript should have operation collapse init")
        assertTrue(html.contains("collapsed-operations"), "JavaScript should use collapsed-operations localStorage key")
    }

    @Test
    fun `should render schema as pretty-printed JSON`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Schema Test API",
                version = "1.0.0"
            ),
            components = Components(
                schemas = mapOf(
                    "User" to Schema(
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("id") {
                                    put("type", "integer")
                                }
                                putJsonObject("name") {
                                    put("type", "string")
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("id"))
                                add(JsonPrimitive("name"))
                            }
                        }
                    )
                )
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // Verify pretty-printed JSON - content is HTML-escaped in the output
        assertTrue(html.contains("language-json"), "Should use language-json CSS class for syntax highlighting hint")
        // In HTML output, quotes are escaped as &quot;
        assertTrue(html.contains("&quot;type&quot;: &quot;object&quot;"), "Should contain pretty-printed type field")
        assertTrue(html.contains("&quot;properties&quot;"), "Should contain properties key")
        assertTrue(html.contains("&quot;id&quot;"), "Should contain id property")
        assertTrue(html.contains("&quot;name&quot;"), "Should contain name property")

        // Should NOT contain raw toString output
        assertFalse(
            html.contains("Schema(schema="),
            "Should not contain raw Schema.toString() output"
        )
    }

    @Test
    fun `should render request body with schema and examples`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Request Body Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/users" to PathItem(
                    post = Operation(
                        summary = "Create user",
                        requestBody = RequestBody(
                            description = "User data",
                            required = true,
                            content = mapOf(
                                "application/json" to MediaType(
                                    schema = Schema(
                                        buildJsonObject {
                                            put("type", "object")
                                            putJsonObject("properties") {
                                                putJsonObject("name") {
                                                    put("type", "string")
                                                }
                                            }
                                        }
                                    ),
                                    example = buildJsonObject {
                                        put("name", "John Doe")
                                    }
                                )
                            )
                        ),
                        responses = mapOf(
                            "201" to Response(
                                description = "Created",
                                content = mapOf(
                                    "application/json" to MediaType(
                                        schema = Schema(
                                            buildJsonObject {
                                                put("type", "object")
                                                putJsonObject("properties") {
                                                    putJsonObject("id") {
                                                        put("type", "integer")
                                                    }
                                                    putJsonObject("name") {
                                                        put("type", "string")
                                                    }
                                                }
                                            }
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val html = renderOpenApiDoc(openapi)

        // Verify request body schema
        assertTrue(html.contains("Request Body"), "Should have Request Body section")
        assertTrue(html.contains("application/json"), "Should show content type")
        assertTrue(html.contains("Required"), "Should show required badge")
        assertTrue(html.contains("schema-display"), "Should have schema display for request body")
        assertTrue(html.contains("Schema"), "Should have Schema heading")

        // Verify example
        assertTrue(html.contains("Example"), "Should have Example heading")
        assertTrue(html.contains("John Doe"), "Should contain example value")

        // Verify response schema (HTML-escaped)
        assertTrue(html.contains("&quot;id&quot;"), "Should show response schema id property")
    }

    @Test
    fun `should include HTTP file download button`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "Download Test API",
                version = "1.0.0"
            ),
            paths = mapOf(
                "/test" to PathItem(
                    get = Operation(
                        summary = "Test",
                        responses = mapOf("200" to Response(description = "OK"))
                    )
                )
            )
        )

        val html = renderOpenApiDoc(openapi)

        assertTrue(html.contains("download-http-file"), "Should have HTTP file download button")
        assertTrue(html.contains("Download .http"), "Should have download button text")
        assertTrue(html.contains("initHttpFileDownload"), "JavaScript should have HTTP file download init")
    }

    @Test
    fun `should not include HTTP file download button for API without paths`() {
        val openapi = OpenAPI(
            openapi = "3.2.0",
            info = Info(
                title = "No Paths API",
                version = "1.0.0"
            ),
            paths = emptyMap()
        )

        val html = renderOpenApiDoc(openapi)

        // The JS code will always contain "download-http-file" as a string reference,
        // but the actual button element should not be present
        assertFalse(
            html.contains("Download .http"),
            "Should not have HTTP file download button text when no paths"
        )
    }
}
