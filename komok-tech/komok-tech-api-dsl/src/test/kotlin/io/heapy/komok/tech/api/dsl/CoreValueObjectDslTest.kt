package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CoreValueObjectDslTest {

    // Contact DSL Tests

    @Test
    fun `contact DSL should create empty Contact`() {
        val result = contact {}

        assertEquals(
            Contact(),
            result
        )
    }

    @Test
    fun `contact DSL should create Contact with all properties`() {
        val result = contact {
            name = "API Support"
            url = "https://example.com/support"
            email = "support@example.com"
            extensions = mapOf("x-internal-id" to JsonPrimitive("12345"))
        }

        assertEquals(
            Contact(
                name = "API Support",
                url = "https://example.com/support",
                email = "support@example.com",
                extensions = mapOf("x-internal-id" to JsonPrimitive("12345"))
            ),
            result
        )
    }

    @Test
    fun `contact DSL should create Contact with partial properties`() {
        val result = contact {
            name = "Support Team"
            email = "team@example.com"
        }

        assertEquals(
            Contact(
                name = "Support Team",
                email = "team@example.com"
            ),
            result
        )
    }

    @Test
    fun `contact DSL should serialize correctly`() {
        val result = contact {
            name = "Support"
            email = "support@example.com"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"name":"Support","email":"support@example.com"}""",
            json
        )
    }

    @Test
    fun `contact DSL should round-trip correctly`() {
        val result = contact {
            name = "API Support"
            url = "https://example.com"
            email = "support@example.com"
        }

        TestHelpers.testRoundTripWithoutValidation(Contact.serializer(), result)
    }

    // License DSL Tests

    @Test
    fun `license DSL should create License with name only`() {
        val result = license {
            name = "Apache 2.0"
        }

        assertEquals(
            License(name = "Apache 2.0"),
            result
        )
    }

    @Test
    fun `license DSL should create License with identifier`() {
        val result = license {
            name = "Apache License 2.0"
            identifier = "Apache-2.0"
        }

        assertEquals(
            License(
                name = "Apache License 2.0",
                identifier = "Apache-2.0"
            ),
            result
        )
    }

    @Test
    fun `license DSL should create License with url`() {
        val result = license {
            name = "MIT"
            url = "https://opensource.org/licenses/MIT"
        }

        assertEquals(
            License(
                name = "MIT",
                url = "https://opensource.org/licenses/MIT"
            ),
            result
        )
    }

    @Test
    fun `license DSL should fail when name is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            license {
                identifier = "MIT"
            }
        }

        assertEquals("License name is required", exception.message)
    }

    @Test
    fun `license DSL should fail when both identifier and url are provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            license {
                name = "MIT"
                identifier = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        assertEquals(
            "License identifier and url are mutually exclusive. Only one should be specified.",
            exception.message
        )
    }

    @Test
    fun `license DSL should serialize correctly`() {
        val result = license {
            name = "Apache 2.0"
            identifier = "Apache-2.0"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"name":"Apache 2.0","identifier":"Apache-2.0"}""",
            json
        )
    }

    @Test
    fun `license DSL should round-trip correctly`() {
        val result = license {
            name = "MIT"
            url = "https://opensource.org/licenses/MIT"
        }

        TestHelpers.testRoundTripWithoutValidation(License.serializer(), result)
    }

    // ExternalDocumentation DSL Tests

    @Test
    fun `externalDocumentation DSL should create with url only`() {
        val result = externalDocumentation {
            url = "https://example.com/docs"
        }

        assertEquals(
            ExternalDocumentation(url = "https://example.com/docs"),
            result
        )
    }

    @Test
    fun `externalDocumentation DSL should create with all properties`() {
        val result = externalDocumentation {
            description = "Find more info here"
            url = "https://example.com/docs"
            extensions = mapOf("x-internal" to JsonPrimitive(true))
        }

        assertEquals(
            ExternalDocumentation(
                description = "Find more info here",
                url = "https://example.com/docs",
                extensions = mapOf("x-internal" to JsonPrimitive(true))
            ),
            result
        )
    }

    @Test
    fun `externalDocumentation DSL should fail when url is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            externalDocumentation {
                description = "Some description"
            }
        }

        assertEquals("ExternalDocumentation url is required", exception.message)
    }

    @Test
    fun `externalDocumentation DSL should serialize correctly`() {
        val result = externalDocumentation {
            description = "API docs"
            url = "https://docs.example.com"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"description":"API docs","url":"https://docs.example.com"}""",
            json
        )
    }

    @Test
    fun `externalDocumentation DSL should round-trip correctly`() {
        val result = externalDocumentation {
            description = "Additional documentation"
            url = "https://docs.example.com"
        }

        TestHelpers.testRoundTripWithoutValidation(ExternalDocumentation.serializer(), result)
    }

    // Tag DSL Tests

    @Test
    fun `tag DSL should create Tag with name only`() {
        val result = tag {
            name = "pets"
        }

        assertEquals(
            Tag(name = "pets"),
            result
        )
    }

    @Test
    fun `tag DSL should create Tag with all properties`() {
        val result = tag {
            name = "pets"
            summary = "Pet operations"
            description = "Everything about your Pets"
            parent = "animals"
            kind = "category"
            extensions = mapOf("x-order" to JsonPrimitive(1))
        }

        assertEquals(
            Tag(
                name = "pets",
                summary = "Pet operations",
                description = "Everything about your Pets",
                parent = "animals",
                kind = "category",
                extensions = mapOf("x-order" to JsonPrimitive(1))
            ),
            result
        )
    }

    @Test
    fun `tag DSL should support nested externalDocs DSL`() {
        val result = tag {
            name = "pets"
            description = "Pet operations"
            externalDocs {
                description = "Find more info here"
                url = "https://example.com/pets"
            }
        }

        assertEquals(
            Tag(
                name = "pets",
                description = "Pet operations",
                externalDocs = ExternalDocumentation(
                    description = "Find more info here",
                    url = "https://example.com/pets"
                )
            ),
            result
        )
    }

    @Test
    fun `tag DSL should fail when name is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            tag {
                description = "Some tag"
            }
        }

        assertEquals("Tag name is required", exception.message)
    }

    @Test
    fun `tag DSL should fail when nested externalDocs has no url`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            tag {
                name = "pets"
                externalDocs {
                    description = "Missing URL"
                }
            }
        }

        assertEquals("ExternalDocumentation url is required", exception.message)
    }

    @Test
    fun `tag DSL should serialize correctly`() {
        val result = tag {
            name = "users"
            summary = "User management"
            description = "Operations for managing users"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"name":"users","summary":"User management","description":"Operations for managing users"}""",
            json
        )
    }

    @Test
    fun `tag DSL should round-trip correctly`() {
        val result = tag {
            name = "pets"
            summary = "Pet operations"
            description = "Everything about your Pets"
            externalDocs {
                url = "https://example.com/pets"
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Tag.serializer(), result)
    }

    // Tags (list) DSL Tests

    @Test
    fun `tags DSL should create list of tags`() {
        val result = tags {
            tag {
                name = "pets"
                description = "Pet operations"
            }
            tag {
                name = "users"
                description = "User operations"
            }
        }

        assertEquals(
            listOf(
                Tag(name = "pets", description = "Pet operations"),
                Tag(name = "users", description = "User operations")
            ),
            result
        )
    }

    @Test
    fun `tags DSL should create empty list`() {
        val result = tags {}

        assertEquals(emptyList<Tag>(), result)
    }

    @Test
    fun `tags DSL should accept pre-built tags`() {
        val preBuiltTag = Tag(name = "orders", description = "Order operations")

        val result = tags {
            tag {
                name = "pets"
            }
            tag(preBuiltTag)
        }

        assertEquals(
            listOf(
                Tag(name = "pets"),
                Tag(name = "orders", description = "Order operations")
            ),
            result
        )
    }

    @Test
    fun `tags DSL should fail if any tag is missing required name`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            tags {
                tag {
                    name = "valid"
                }
                tag {
                    description = "Missing name"
                }
            }
        }

        assertEquals("Tag name is required", exception.message)
    }

    @Test
    fun `tags DSL should support nested externalDocs`() {
        val result = tags {
            tag {
                name = "pets"
                externalDocs {
                    url = "https://example.com/pets"
                }
            }
        }

        assertEquals(
            listOf(
                Tag(
                    name = "pets",
                    externalDocs = ExternalDocumentation(url = "https://example.com/pets")
                )
            ),
            result
        )
    }
}
