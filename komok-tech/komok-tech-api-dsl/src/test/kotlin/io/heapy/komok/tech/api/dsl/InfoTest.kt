package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InfoTest {

    // Basic Info Tests

    @Test
    fun `should serialize minimal Info`() {
        val info = Info(
            title = "Sample API",
            version = "1.0.0"
        )
        val json = compactJson.encodeToString(info)

        val expected = """{"title":"Sample API","version":"1.0.0"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Info with summary`() {
        val info = Info(
            title = "Pet Store API",
            version = "1.0.0",
            summary = "A sample pet store API"
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("\"title\":\"Pet Store API\""))
        assertTrue(json.contains("\"version\":\"1.0.0\""))
        assertTrue(json.contains("\"summary\":\"A sample pet store API\""))
    }

    @Test
    fun `should serialize Info with description`() {
        val info = Info(
            title = "User API",
            version = "2.1.0",
            description = "This is a comprehensive API for managing users in the system. It provides endpoints for CRUD operations."
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("description"))
        assertTrue(json.contains("comprehensive API"))
    }

    @Test
    fun `should serialize Info with termsOfService`() {
        val info = Info(
            title = "API",
            version = "1.0.0",
            termsOfService = "https://example.com/terms"
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("termsOfService"))
        assertTrue(json.contains("https://example.com/terms"))
    }

    @Test
    fun `should serialize Info with contact`() {
        val info = Info(
            title = "API",
            version = "1.0.0",
            contact = Contact(
                name = "API Support",
                url = "https://example.com/support",
                email = "support@example.com"
            )
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("contact"))
        assertTrue(json.contains("API Support"))
        assertTrue(json.contains("support@example.com"))
    }

    @Test
    fun `should serialize Info with license`() {
        val info = Info(
            title = "Open API",
            version = "1.0.0",
            license = License(
                name = "Apache 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0.html"
            )
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("license"))
        assertTrue(json.contains("Apache 2.0"))
    }

    @Test
    fun `should serialize Info with MIT license using identifier`() {
        val info = Info(
            title = "Open Source API",
            version = "1.0.0",
            license = License(
                name = "MIT",
                identifier = "MIT"
            )
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("MIT"))
        assertTrue(json.contains("identifier"))
    }

    @Test
    fun `should serialize Info with all properties`() {
        val info = Info(
            title = "Comprehensive API",
            version = "3.0.0",
            summary = "A complete example API",
            description = "This API demonstrates all Info object properties",
            termsOfService = "https://example.com/terms",
            contact = Contact(
                name = "API Team",
                url = "https://example.com/contact",
                email = "api@example.com"
            ),
            license = License(
                name = "Apache 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0.html"
            )
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("Comprehensive API"))
        assertTrue(json.contains("3.0.0"))
        assertTrue(json.contains("complete example"))
        assertTrue(json.contains("terms"))
        assertTrue(json.contains("API Team"))
        assertTrue(json.contains("Apache 2.0"))
    }

    @Test
    fun `should serialize Info with specification extensions`() {
        val info = Info(
            title = "Custom API",
            version = "1.0.0",
            extensions = mapOf(
                "x-api-id" to JsonPrimitive("api-12345"),
                "x-audience" to JsonPrimitive("external")
            )
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("x-api-id"))
        assertTrue(json.contains("api-12345"))
        assertTrue(json.contains("x-audience"))
    }

    // Version Format Tests

    @Test
    fun `should serialize Info with semantic version`() {
        val info = Info(
            title = "API",
            version = "1.2.3"
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("\"version\":\"1.2.3\""))
    }

    @Test
    fun `should serialize Info with prerelease version`() {
        val info = Info(
            title = "API",
            version = "2.0.0-beta.1"
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("2.0.0-beta.1"))
    }

    @Test
    fun `should serialize Info with build metadata in version`() {
        val info = Info(
            title = "API",
            version = "1.0.0+20130313144700"
        )
        val json = compactJson.encodeToString(info)

        assertTrue(json.contains("1.0.0+20130313144700"))
    }

    // Deserialization Tests

    @Test
    fun `should deserialize Info`() {
        val json = """{"title":"Test API","version":"1.0.0","summary":"Test"}"""
        val info = compactJson.decodeFromString<Info>(json)

        assertEquals(
            Info(
                title = "Test API",
                version = "1.0.0",
                summary = "Test"
            ),
            info
        )
    }

    @Test
    fun `should deserialize Info with contact and license`() {
        val json = """{"title":"API","version":"1.0.0","contact":{"name":"Support"},"license":{"name":"MIT"}}"""
        val info = compactJson.decodeFromString<Info>(json)

        assertEquals(
            Info(
                title = "API",
                version = "1.0.0",
                contact = Contact(name = "Support"),
                license = License(name = "MIT")
            ),
            info
        )
    }

    // Round-trip Tests

    @Test
    fun `should round-trip minimal Info`() {
        val info = Info(
            title = "Sample API",
            version = "1.0.0"
        )

        TestHelpers.testRoundTripWithoutValidation(Info.serializer(), info)
    }

    @Test
    fun `should round-trip Info with all properties`() {
        val info = Info(
            title = "Complete API",
            version = "2.5.1",
            summary = "API Summary",
            description = "Detailed API description with CommonMark **formatting**",
            termsOfService = "https://example.com/terms",
            contact = Contact(
                name = "API Support Team",
                url = "https://example.com/support",
                email = "support@example.com"
            ),
            license = License(
                name = "Apache 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0.html"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Info.serializer(), info)
    }

    // Real-world Examples

    @Test
    fun `should serialize Stripe-style Info`() {
        val info = Info(
            title = "Stripe API",
            version = "2023-10-16",
            description = "The Stripe REST API. Please see https://stripe.com/docs/api for more details.",
            termsOfService = "https://stripe.com/terms",
            contact = Contact(
                name = "Stripe Dev Platform Team",
                email = "dev-platform@stripe.com",
                url = "https://support.stripe.com/"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Info.serializer(), info)
    }

    @Test
    fun `should serialize GitHub-style Info`() {
        val info = Info(
            title = "GitHub v3 REST API",
            version = "1.1.4",
            summary = "GitHub's v3 REST API",
            description = "GitHub's v3 REST API.",
            license = License(
                name = "MIT",
                url = "https://spdx.org/licenses/MIT"
            ),
            termsOfService = "https://docs.github.com/articles/github-terms-of-service",
            contact = Contact(
                name = "Support",
                url = "https://support.github.com/contact?tags=dotcom-rest-api"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Info.serializer(), info)
    }

    @Test
    fun `should serialize Petstore-style Info`() {
        val info = Info(
            title = "Swagger Petstore - OpenAPI 3.2",
            version = "1.0.11",
            summary = "This is a sample Pet Store Server based on the OpenAPI 3.2 specification",
            description = """
                This is a sample Pet Store Server based on the OpenAPI 3.2 specification.
                You can find out more about Swagger at [https://swagger.io](https://swagger.io).
            """.trimIndent(),
            termsOfService = "http://swagger.io/terms/",
            contact = Contact(
                email = "apiteam@swagger.io"
            ),
            license = License(
                name = "Apache 2.0",
                url = "http://www.apache.org/licenses/LICENSE-2.0.html"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Info.serializer(), info)
    }
}
