package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.*
import io.heapy.komok.tech.logging.Logger
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
                            Parameter(
                                name = "limit",
                                location = ParameterLocation.QUERY,
                                description = "Maximum number of users to return",
                                required = false,
                                schema = Schema(buildJsonObject { put("type", "integer") })
                            ),
                            Parameter(
                                name = "offset",
                                location = ParameterLocation.QUERY,
                                description = "Number of users to skip",
                                required = false,
                                schema = Schema(buildJsonObject { put("type", "integer") })
                            ),
                            Parameter(
                                name = "Authorization",
                                location = ParameterLocation.HEADER,
                                description = "Bearer token",
                                required = true,
                                schema = Schema(buildJsonObject { put("type", "string") })
                            )
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
}
