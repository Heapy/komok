package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BasicInformationObjectsTest {

    // Contact Tests

    @Test
    fun `should serialize minimal Contact object`() {
        val contact = Contact(name = "API Support")
        val json = compactJson.encodeToString(contact)

        assertEquals("""{"name":"API Support"}""", json)
    }

    @Test
    fun `should serialize full Contact object`() {
        val contact = Contact(
            name = "API Support Team",
            url = "https://example.com/support",
            email = "support@example.com"
        )
        val json = compactJson.encodeToString(contact)

        assertEquals(
            """{"name":"API Support Team","url":"https://example.com/support","email":"support@example.com"}""",
            json
        )
    }

    @Test
    fun `should serialize Contact with extensions`() {
        val contact = Contact(
            name = "Support",
            extensions = mapOf("x-internal-id" to JsonPrimitive("12345"))
        )
        val json = compactJson.encodeToString(contact)

        assertEquals(
            """{"name":"Support","extensions":{"x-internal-id":"12345"}}""",
            json
        )
    }

    @Test
    fun `should deserialize Contact object`() {
        val json = """{"name":"Support","email":"support@example.com"}"""
        val contact = compactJson.decodeFromString<Contact>(json)

        assertEquals("Support", contact.name)
        assertEquals("support@example.com", contact.email)
    }

    @Test
    fun `should round-trip Contact object`() {
        val contact = Contact(
            name = "Support",
            url = "https://example.com",
            email = "support@example.com"
        )

        TestHelpers.testRoundTripWithoutValidation(Contact.serializer(), contact)
    }

    // License Tests

    @Test
    fun `should serialize License with name only`() {
        val license = License(name = "Apache 2.0")
        val json = compactJson.encodeToString(license)

        assertEquals("""{"name":"Apache 2.0"}""", json)
    }

    @Test
    fun `should serialize License with identifier`() {
        val license = License(
            name = "Apache License 2.0",
            identifier = "Apache-2.0"
        )
        val json = compactJson.encodeToString(license)

        assertEquals(
            """{"name":"Apache License 2.0","identifier":"Apache-2.0"}""",
            json
        )
    }

    @Test
    fun `should serialize License with url`() {
        val license = License(
            name = "MIT",
            url = "https://opensource.org/licenses/MIT"
        )
        val json = compactJson.encodeToString(license)

        assertEquals(
            """{"name":"MIT","url":"https://opensource.org/licenses/MIT"}""",
            json
        )
    }

    @Test
    fun `should reject License with both identifier and url`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            License(
                name = "MIT",
                identifier = "MIT",
                url = "https://opensource.org/licenses/MIT"
            )
        }

        assertEquals(
            "License identifier and url are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `should round-trip License object`() {
        val license = License(
            name = "Apache 2.0",
            identifier = "Apache-2.0"
        )

        TestHelpers.testRoundTripWithoutValidation(License.serializer(), license)
    }

    // ExternalDocumentation Tests

    @Test
    fun `should serialize ExternalDocumentation with url only`() {
        val externalDocs = ExternalDocumentation(url = "https://example.com/docs")
        val json = compactJson.encodeToString(externalDocs)

        assertEquals(
            """{"url":"https://example.com/docs"}""",
            json
        )
    }

    @Test
    fun `should serialize ExternalDocumentation with description`() {
        val externalDocs = ExternalDocumentation(
            description = "Find more info here",
            url = "https://example.com/docs"
        )
        val json = compactJson.encodeToString(externalDocs)

        assertEquals(
            """{"description":"Find more info here","url":"https://example.com/docs"}""",
            json
        )
    }

    @Test
    fun `should round-trip ExternalDocumentation object`() {
        val externalDocs = ExternalDocumentation(
            description = "Additional documentation",
            url = "https://docs.example.com"
        )

        TestHelpers.testRoundTripWithoutValidation(ExternalDocumentation.serializer(), externalDocs)
    }

    // Tag Tests

    @Test
    fun `should serialize Tag with name only`() {
        val tag = Tag(name = "pets")
        val json = compactJson.encodeToString(tag)

        assertEquals("""{"name":"pets"}""", json)
    }

    @Test
    fun `should serialize Tag with all properties`() {
        val tag = Tag(
            name = "pets",
            summary = "Pet operations",
            description = "Everything about your Pets",
            externalDocs = ExternalDocumentation(url = "https://example.com/pets"),
            parent = "animals",
            kind = "category"
        )
        val json = compactJson.encodeToString(tag)

        val expected = """{"name":"pets","summary":"Pet operations","description":"Everything about your Pets","externalDocs":{"url":"https://example.com/pets"},"parent":"animals","kind":"category"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should round-trip Tag object`() {
        val tag = Tag(
            name = "users",
            summary = "User management",
            description = "Operations related to users"
        )

        TestHelpers.testRoundTripWithoutValidation(Tag.serializer(), tag)
    }

    // Reference Tests

    @Test
    fun `should serialize Reference with ref only`() {
        val reference = Reference(ref = "#/components/schemas/Pet")
        val json = compactJson.encodeToString(reference)

        assertEquals(
            """{"${'$'}ref":"#/components/schemas/Pet"}""",
            json
        )
    }

    @Test
    fun `should serialize Reference with summary and description`() {
        val reference = Reference(
            ref = "#/components/schemas/User",
            summary = "User reference",
            description = "Reference to a User schema"
        )
        val json = compactJson.encodeToString(reference)

        val expected = """{"${'$'}ref":"#/components/schemas/User","summary":"User reference","description":"Reference to a User schema"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should deserialize Reference object`() {
        val json = """{"${'$'}ref":"#/components/schemas/Pet","summary":"Pet reference"}"""
        val reference = compactJson.decodeFromString<Reference>(json)

        assertEquals("#/components/schemas/Pet", reference.ref)
        assertEquals("Pet reference", reference.summary)
    }

    @Test
    fun `should round-trip Reference object`() {
        val reference = Reference(
            ref = "#/components/parameters/limit",
            description = "Common limit parameter"
        )

        TestHelpers.testRoundTripWithoutValidation(Reference.serializer(), reference)
    }
}
