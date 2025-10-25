package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerObjectsTest {

    // ServerVariable Tests

    @Test
    fun `should serialize ServerVariable with default only`() {
        val serverVariable = ServerVariable(default = "v1")
        val json = compactJson.encodeToString(serverVariable)

        assertEquals("""{"default":"v1"}""", json)
    }

    @Test
    fun `should serialize ServerVariable with enum`() {
        val serverVariable = ServerVariable(
            default = "production",
            enum = listOf("production", "staging", "development")
        )
        val json = compactJson.encodeToString(serverVariable)

        val expected = """{"default":"production","enum":["production","staging","development"]}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize ServerVariable with description`() {
        val serverVariable = ServerVariable(
            default = "8443",
            description = "Secure port number"
        )
        val json = compactJson.encodeToString(serverVariable)

        val expected = """{"default":"8443","description":"Secure port number"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize ServerVariable with all properties`() {
        val serverVariable = ServerVariable(
            default = "v2",
            enum = listOf("v1", "v2", "v3"),
            description = "API version",
            extensions = mapOf("x-deprecated" to JsonPrimitive(false))
        )
        val json = compactJson.encodeToString(serverVariable)

        val expected = """{"default":"v2","enum":["v1","v2","v3"],"description":"API version","extensions":{"x-deprecated":false}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should reject ServerVariable with empty enum`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            ServerVariable(
                default = "v1",
                enum = emptyList()
            )
        }

        assertEquals("ServerVariable enum must contain at least one value", exception.message)
    }

    @Test
    fun `should deserialize ServerVariable`() {
        val json = """{"default":"production","enum":["production","staging"]}"""
        val serverVariable = compactJson.decodeFromString<ServerVariable>(json)

        assertEquals("production", serverVariable.default)
        assertEquals(listOf("production", "staging"), serverVariable.enum)
    }

    @Test
    fun `should round-trip ServerVariable object`() {
        val serverVariable = ServerVariable(
            default = "8080",
            enum = listOf("8080", "8443"),
            description = "Port number"
        )

        TestHelpers.testRoundTripWithoutValidation(ServerVariable.serializer(), serverVariable)
    }

    // Server Tests

    @Test
    fun `should serialize Server with url only`() {
        val server = Server(url = "https://api.example.com")
        val json = compactJson.encodeToString(server)

        assertEquals("""{"url":"https://api.example.com"}""", json)
    }

    @Test
    fun `should serialize Server with description`() {
        val server = Server(
            url = "https://api.example.com",
            description = "Production server"
        )
        val json = compactJson.encodeToString(server)

        val expected = """{"url":"https://api.example.com","description":"Production server"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Server with name`() {
        val server = Server(
            url = "https://api.example.com",
            name = "production"
        )
        val json = compactJson.encodeToString(server)

        val expected = """{"url":"https://api.example.com","name":"production"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Server with variables`() {
        val server = Server(
            url = "https://{environment}.example.com:{port}/{basePath}",
            variables = mapOf(
                "environment" to ServerVariable(
                    default = "api",
                    enum = listOf("api", "api-staging")
                ),
                "port" to ServerVariable(
                    default = "8443"
                ),
                "basePath" to ServerVariable(
                    default = "v2"
                )
            )
        )
        val json = compactJson.encodeToString(server)

        val expected = """{"url":"https://{environment}.example.com:{port}/{basePath}","variables":{"environment":{"default":"api","enum":["api","api-staging"]},"port":{"default":"8443"},"basePath":{"default":"v2"}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Server with all properties`() {
        val server = Server(
            url = "https://api.example.com/v1",
            description = "Main production server",
            name = "production",
            variables = mapOf(
                "version" to ServerVariable(default = "v1")
            ),
            extensions = mapOf("x-region" to JsonPrimitive("us-east-1"))
        )
        val json = compactJson.encodeToString(server)

        val expected = """{"url":"https://api.example.com/v1","description":"Main production server","name":"production","variables":{"version":{"default":"v1"}},"extensions":{"x-region":"us-east-1"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Server with relative url`() {
        val server = Server(url = "/api")
        val json = compactJson.encodeToString(server)

        assertEquals("""{"url":"/api"}""", json)
    }

    @Test
    fun `should deserialize Server`() {
        val json = """{"url":"https://api.example.com","description":"Production"}"""
        val server = compactJson.decodeFromString<Server>(json)

        assertEquals("https://api.example.com", server.url)
        assertEquals("Production", server.description)
    }

    @Test
    fun `should round-trip Server object`() {
        val server = Server(
            url = "https://api.example.com",
            description = "API Server",
            name = "main"
        )

        TestHelpers.testRoundTripWithoutValidation(Server.serializer(), server)
    }

    @Test
    fun `should round-trip Server with variables`() {
        val server = Server(
            url = "https://{environment}.example.com",
            variables = mapOf(
                "environment" to ServerVariable(
                    default = "api",
                    enum = listOf("api", "staging"),
                    description = "Environment name"
                )
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Server.serializer(), server)
    }
}
