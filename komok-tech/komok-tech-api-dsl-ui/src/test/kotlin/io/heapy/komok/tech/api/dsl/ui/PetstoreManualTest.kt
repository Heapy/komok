package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.OpenAPI
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Manual test to generate HTML documentation from the Petstore OpenAPI example.
 *
 * This test reads the petstore-full-3.0.0.json file, converts it to an OpenAPI object,
 * and generates a complete HTML documentation page that can be opened in a browser.
 *
 * The generated HTML file will be written to:
 * komok-tech/komok-tech-api-dsl-ui/build/petstore-documentation.html
 */
class PetstoreManualTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `generate HTML documentation from Petstore OpenAPI spec`() {
        // Read the Petstore OpenAPI JSON file
        val petstoreJsonPath = Paths.get("../komok-tech-api-dsl/src/test/resources/petstore-full-3.0.0.json")
        val petstoreJson = petstoreJsonPath.readText()

        println("Read Petstore OpenAPI spec from: ${petstoreJsonPath.toAbsolutePath()}")
        println("JSON size: ${petstoreJson.length} characters")

        // Note: The petstore file is OpenAPI 3.0.0, but we need 3.2.0
        // We'll need to update the version in the JSON before parsing
        val updatedJson = petstoreJson.replace("\"openapi\": \"3.0.0\"", "\"openapi\": \"3.2.0\"")

        // Deserialize to OpenAPI object
        val openapi = try {
            json.decodeFromString<OpenAPI>(updatedJson)
        } catch (e: Exception) {
            println("Failed to deserialize OpenAPI spec: ${e.message}")
            e.printStackTrace()
            throw e
        }

        println("Successfully deserialized OpenAPI document")
        println("  Title: ${openapi.info.title}")
        println("  Version: ${openapi.info.version}")
        println("  Paths: ${openapi.paths?.size ?: 0}")
        println("  Schemas: ${openapi.components?.schemas?.size ?: 0}")

        // Generate HTML documentation
        val html = renderOpenApiDoc(openapi)

        println("Generated HTML documentation")
        println("  HTML size: ${html.length} characters")
        println("  HTML lines: ${html.lines().size}")

        // Write HTML to file
        val outputPath = Paths.get("build/petstore-documentation.html")
        Files.createDirectories(outputPath.parent)
        outputPath.writeText(html)

        println("Wrote HTML documentation to: ${outputPath.toAbsolutePath()}")
        println("")
        println("To view the documentation, open the following file in your browser:")
        println("  file://${outputPath.toAbsolutePath()}")

        // Basic validation
        assert(html.contains("Swagger Petstore")) { "HTML should contain Petstore title" }
        assert(html.contains("pet")) { "HTML should contain pet tag" }
        assert(html.contains("store")) { "HTML should contain store tag" }
        assert(html.contains("user")) { "HTML should contain user tag" }
        assert(html.contains("/pet")) { "HTML should contain /pet path" }
        assert(html.contains("GET")) { "HTML should contain GET method" }
        assert(html.contains("POST")) { "HTML should contain POST method" }
        assert(html.contains("PUT")) { "HTML should contain PUT method" }
        assert(html.contains("DELETE")) { "HTML should contain DELETE method" }

        println("✓ All validations passed")
    }

    @Test
    fun `generate minimal example HTML documentation`() {
        // Create a minimal OpenAPI example
        val minimalOpenApi = OpenAPI(
            openapi = "3.2.0",
            info = io.heapy.komok.tech.api.dsl.Info(
                title = "Minimal API Example",
                version = "1.0.0",
                description = "A minimal example showing the UI renderer capabilities",
                contact = io.heapy.komok.tech.api.dsl.Contact(
                    name = "API Team",
                    email = "api@example.com"
                )
            ),
            servers = listOf(
                io.heapy.komok.tech.api.dsl.Server(
                    url = "https://api.example.com",
                    description = "Production server"
                ),
                io.heapy.komok.tech.api.dsl.Server(
                    url = "https://staging.api.example.com",
                    description = "Staging server"
                )
            ),
            paths = mapOf(
                "/hello" to io.heapy.komok.tech.api.dsl.PathItem(
                    get = io.heapy.komok.tech.api.dsl.Operation(
                        summary = "Say Hello",
                        description = "Returns a greeting message",
                        responses = mapOf(
                            "200" to io.heapy.komok.tech.api.dsl.Response(
                                summary = "Success",
                                description = "A greeting message"
                            )
                        )
                    )
                )
            )
        )

        // Generate HTML
        val html = renderOpenApiDoc(minimalOpenApi)

        // Write to file
        val outputPath = Paths.get("build/minimal-example-documentation.html")
        Files.createDirectories(outputPath.parent)
        outputPath.writeText(html)

        println("Generated minimal example HTML documentation")
        println("  Output: ${outputPath.toAbsolutePath()}")
        println("  To view: file://${outputPath.toAbsolutePath()}")

        // Validation
        assert(html.contains("Minimal API Example")) { "Should contain title" }
        assert(html.contains("/hello")) { "Should contain endpoint" }
        assert(html.contains("Say Hello")) { "Should contain operation summary" }
    }
}
