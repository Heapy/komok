package io.heapy.komok.tech.api.dsl.ui

import com.charleskorn.kaml.AnchorsAndAliases
import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import io.heapy.komok.tech.api.dsl.OpenAPI
import io.heapy.komok.tech.logging.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Integration tests for parsing GitHub REST API OpenAPI specifications
 * and generating HTML documentation from them.
 *
 * These tests download the official GitHub REST API specifications if not present,
 * parse them (JSON and YAML formats), and generate HTML documentation.
 */
class GitHubApiIntegrationTest {

    private companion object : Logger() {
        private val CACHE_DIR: Path = Paths.get("build/github-api-specs")

        private val API_SPECS = listOf(
            ApiSpec(
                name = "api.github.com.yaml",
                url = "https://github.com/github/rest-api-description/raw/refs/heads/main/descriptions-next/api.github.com/api.github.com.yaml",
                format = Format.YAML
            ),
            ApiSpec(
                name = "api.github.com.json",
                url = "https://github.com/github/rest-api-description/raw/refs/heads/main/descriptions-next/api.github.com/api.github.com.json",
                format = Format.JSON
            ),
            ApiSpec(
                name = "api.github.com.deref.json",
                url = "https://github.com/github/rest-api-description/raw/refs/heads/main/descriptions-next/api.github.com/dereferenced/api.github.com.deref.json",
                format = Format.JSON
            ),
            ApiSpec(
                name = "api.github.com.deref.yaml",
                url = "https://github.com/github/rest-api-description/raw/refs/heads/main/descriptions-next/api.github.com/dereferenced/api.github.com.deref.yaml",
                format = Format.YAML
            )
        )

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private val yaml = Yaml(
            configuration = YamlConfiguration(
                strictMode = false,
                anchorsAndAliases = AnchorsAndAliases.Permitted(
                    maxAliasCount = 1_000_000u,
                ),
                codePointLimit = Int.MAX_VALUE,
                polymorphismStyle = PolymorphismStyle.Property,
            )
        )

        @JvmStatic
        @BeforeAll
        fun downloadSpecs() {
            Files.createDirectories(CACHE_DIR)

            API_SPECS.forEach { spec ->
                val localPath = CACHE_DIR.resolve(spec.name)
                if (!localPath.exists()) {
                    log.info("Downloading ${spec.name} from ${spec.url}")
                    downloadFile(spec.url, localPath)
                    log.info("Downloaded ${spec.name} (${Files.size(localPath)} bytes)")
                } else {
                    log.info("Using cached ${spec.name}")
                }
            }
        }

        private fun downloadFile(url: String, destination: Path) {
            URI(url).toURL().openStream().use { input ->
                Files.newOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }

        /**
         * Updates the OpenAPI version to 3.2.0 to match the library requirements.
         * GitHub API uses OpenAPI 3.1.x which needs to be patched for compatibility.
         */
        private fun patchOpenApiVersion(content: String): String {
            return content
                .replace(Regex(""""openapi"\s*:\s*"3\.[01]\.\d+""""), """"openapi": "3.2.0"""")
                .replace(Regex("""openapi:\s*['"]?3\.[01]\.\d+['"]?"""), """openapi: "3.2.0"""")
        }

        /**
         * Converts YAML content to JSON string using kaml.
         * This is necessary because Schema uses JsonElement which only works with JSON format.
         */
        private fun yamlToJson(yamlContent: String): String {
            val yamlNode = yaml.parseToYamlNode(yamlContent)
            val jsonElement = yamlNodeToJsonElement(yamlNode)
            return Json.encodeToString(JsonElement.serializer(), jsonElement)
        }

        private fun yamlNodeToJsonElement(node: YamlNode): JsonElement = when (node) {
            is YamlNull -> JsonNull
            is YamlScalar -> yamlScalarToJsonPrimitive(node)
            is YamlList -> JsonArray(node.items.map { yamlNodeToJsonElement(it) })
            is YamlMap -> JsonObject(
                node.entries.map { (key, value) ->
                    key.content to yamlNodeToJsonElement(value)
                }.toMap()
            )
            is YamlTaggedNode -> yamlNodeToJsonElement(node.innerNode)
        }

        private fun yamlScalarToJsonPrimitive(scalar: YamlScalar): JsonPrimitive {
            val content = scalar.content
            return when {
                content == "true" -> JsonPrimitive(true)
                content == "false" -> JsonPrimitive(false)
                content == "null" -> JsonPrimitive(null as String?)
                content.toLongOrNull() != null -> JsonPrimitive(content.toLong())
                content.toDoubleOrNull() != null -> JsonPrimitive(content.toDouble())
                else -> JsonPrimitive(content)
            }
        }
    }

    private enum class Format { JSON, YAML }

    private data class ApiSpec(
        val name: String,
        val url: String,
        val format: Format
    )

    @Test
    fun `parse GitHub API JSON spec and generate UI`() {
        val spec = API_SPECS.first { it.name == "api.github.com.json" }
        val localPath = CACHE_DIR.resolve(spec.name)
        val content = patchOpenApiVersion(localPath.readText())

        log.info("Parsing ${spec.name}...")
        val openapi = json.decodeFromString<OpenAPI>(content)

        log.info("Parsed OpenAPI document:")
        log.info("  Title: ${openapi.info.title}")
        log.info("  Version: ${openapi.info.version}")
        log.info("  Paths: ${openapi.paths?.size ?: 0}")
        log.info("  Schemas: ${openapi.components?.schemas?.size ?: 0}")

        val html = renderOpenApiDoc(openapi)

        val outputPath = CACHE_DIR.resolve("github-api-json-documentation.html")
        outputPath.writeText(html)
        log.info("Generated HTML documentation: ${outputPath.toAbsolutePath()}")

        assertTrue(openapi.info.title.contains("GitHub"), "Should be GitHub API")
        assertTrue(openapi.paths?.isNotEmpty() == true, "Should have paths")
        assertTrue(html.contains("GitHub"), "HTML should contain GitHub")
        assertTrue(html.contains("<html"), "Should be valid HTML")
    }

    @Test
    fun `parse GitHub API YAML spec and generate UI`() {
        val spec = API_SPECS.first { it.name == "api.github.com.yaml" }
        val localPath = CACHE_DIR.resolve(spec.name)
        val yamlContent = patchOpenApiVersion(localPath.readText())

        log.info("Converting YAML to JSON...")
        val jsonContent = yamlToJson(yamlContent)

        log.info("Parsing ${spec.name}...")
        val openapi = yaml.decodeFromString(OpenAPI.serializer(), jsonContent)

        log.info("Parsed OpenAPI document:")
        log.info("  Title: ${openapi.info.title}")
        log.info("  Version: ${openapi.info.version}")
        log.info("  Paths: ${openapi.paths?.size ?: 0}")
        log.info("  Schemas: ${openapi.components?.schemas?.size ?: 0}")

        val html = renderOpenApiDoc(openapi)

        val outputPath = CACHE_DIR.resolve("github-api-yaml-documentation.html")
        outputPath.writeText(html)
        log.info("Generated HTML documentation: ${outputPath.toAbsolutePath()}")

        assertTrue(openapi.info.title.contains("GitHub"), "Should be GitHub API")
        assertTrue(openapi.paths?.isNotEmpty() == true, "Should have paths")
        assertTrue(html.contains("GitHub"), "HTML should contain GitHub")
        assertTrue(html.contains("<html"), "Should be valid HTML")
    }

    /**
     * Test parsing the dereferenced JSON spec and generating HTML documentation.
     * This is the primary working test - the dereferenced JSON file has all $ref
     * references resolved, making it compatible with the current model.
     */
    @Test
    fun `parse GitHub API dereferenced JSON spec and generate UI`() {
        val spec = API_SPECS.first { it.name == "api.github.com.deref.json" }
        val localPath = CACHE_DIR.resolve(spec.name)
        val content = patchOpenApiVersion(localPath.readText())

        log.info("Parsing ${spec.name}...")
        val openapi = json.decodeFromString<OpenAPI>(content)

        log.info("Parsed OpenAPI document:")
        log.info("  Title: ${openapi.info.title}")
        log.info("  Version: ${openapi.info.version}")
        log.info("  Paths: ${openapi.paths?.size ?: 0}")
        log.info("  Schemas: ${openapi.components?.schemas?.size ?: 0}")

        val html = renderOpenApiDoc(openapi)

        val outputPath = CACHE_DIR.resolve("github-api-deref-json-documentation.html")
        outputPath.writeText(html)
        log.info("Generated HTML documentation: ${outputPath.toAbsolutePath()}")

        assertTrue(openapi.info.title.contains("GitHub"), "Should be GitHub API")
        assertTrue(openapi.paths?.isNotEmpty() == true, "Should have paths")
        assertTrue(html.contains("GitHub"), "HTML should contain GitHub")
        assertTrue(html.contains("<html"), "Should be valid HTML")
    }

    @Test
    fun `parse GitHub API dereferenced YAML spec and generate UI`() {
        val spec = API_SPECS.first { it.name == "api.github.com.deref.yaml" }
        val localPath = CACHE_DIR.resolve(spec.name)
        val yamlContent = patchOpenApiVersion(localPath.readText())

        log.info("Converting YAML to JSON...")
        val jsonContent = yamlToJson(yamlContent)

        log.info("Parsing ${spec.name}...")
        val openapi = json.decodeFromString<OpenAPI>(jsonContent)

        log.info("Parsed OpenAPI document:")
        log.info("  Title: ${openapi.info.title}")
        log.info("  Version: ${openapi.info.version}")
        log.info("  Paths: ${openapi.paths?.size ?: 0}")
        log.info("  Schemas: ${openapi.components?.schemas?.size ?: 0}")

        val html = renderOpenApiDoc(openapi)

        val outputPath = CACHE_DIR.resolve("github-api-deref-yaml-documentation.html")
        outputPath.writeText(html)
        log.info("Generated HTML documentation: ${outputPath.toAbsolutePath()}")

        assertTrue(openapi.info.title.contains("GitHub"), "Should be GitHub API")
        assertTrue(openapi.paths?.isNotEmpty() == true, "Should have paths")
        assertTrue(html.contains("GitHub"), "HTML should contain GitHub")
        assertTrue(html.contains("<html"), "Should be valid HTML")
    }
}
