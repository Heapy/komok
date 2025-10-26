package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ContentAndMediaTypeTest {

    // Encoding Tests

    @Test
    fun `should serialize minimal Encoding`() {
        val encoding = Encoding(contentType = "application/json")
        val json = compactJson.encodeToString(encoding)

        assertEquals("""{"contentType":"application/json"}""", json)
    }

    @Test
    fun `should serialize Encoding with style`() {
        val encoding = Encoding(
            contentType = "application/json",
            style = EncodingStyle.FORM
        )
        val json = compactJson.encodeToString(encoding)

        assertEquals("""{"contentType":"application/json","style":"form"}""", json)
    }

    @Test
    fun `should serialize Encoding with all style types`() {
        val styles = listOf(
            EncodingStyle.FORM to "form",
            EncodingStyle.SPACE_DELIMITED to "spaceDelimited",
            EncodingStyle.PIPE_DELIMITED to "pipeDelimited",
            EncodingStyle.DEEP_OBJECT to "deepObject"
        )

        styles.forEach { (style, expectedName) ->
            val encoding = Encoding(style = style)
            val json = compactJson.encodeToString(encoding)
            assertEquals("""{"style":"$expectedName"}""", json)
        }
    }

    @Test
    fun `should serialize Encoding with explode and allowReserved`() {
        val encoding = Encoding(
            style = EncodingStyle.FORM,
            explode = true,
            allowReserved = false
        )
        val json = compactJson.encodeToString(encoding)

        assertEquals("""{"style":"form","explode":true,"allowReserved":false}""", json)
    }

    @Test
    fun `should serialize Encoding with nested encoding map`() {
        val encoding = Encoding(
            contentType = "application/json",
            encoding = mapOf(
                "address" to Encoding(
                    contentType = "application/json",
                    style = EncodingStyle.DEEP_OBJECT
                ),
                "tags" to Encoding(
                    style = EncodingStyle.SPACE_DELIMITED,
                    explode = false
                )
            )
        )
        val json = compactJson.encodeToString(encoding)

        val expected = """{"contentType":"application/json","encoding":{"address":{"contentType":"application/json","style":"deepObject"},"tags":{"style":"spaceDelimited","explode":false}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Encoding with prefixEncoding`() {
        val encoding = Encoding(
            contentType = "application/json",
            prefixEncoding = listOf(
                Encoding(style = EncodingStyle.FORM),
                Encoding(style = EncodingStyle.PIPE_DELIMITED)
            )
        )
        val json = compactJson.encodeToString(encoding)

        val expected = """{"contentType":"application/json","prefixEncoding":[{"style":"form"},{"style":"pipeDelimited"}]}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Encoding with itemEncoding`() {
        val encoding = Encoding(
            contentType = "application/json",
            itemEncoding = Encoding(
                style = EncodingStyle.SPACE_DELIMITED,
                explode = true
            )
        )
        val json = compactJson.encodeToString(encoding)

        val expected = """{"contentType":"application/json","itemEncoding":{"style":"spaceDelimited","explode":true}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should reject Encoding with both encoding and prefixEncoding`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Encoding(
                encoding = mapOf("field" to Encoding(style = EncodingStyle.FORM)),
                prefixEncoding = listOf(Encoding(style = EncodingStyle.FORM))
            )
        }

        assertEquals(
            "Encoding 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `should reject Encoding with both encoding and itemEncoding`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Encoding(
                encoding = mapOf("field" to Encoding(style = EncodingStyle.FORM)),
                itemEncoding = Encoding(style = EncodingStyle.FORM)
            )
        }

        assertEquals(
            "Encoding 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `should allow Encoding with both prefixEncoding and itemEncoding`() {
        // prefixEncoding and itemEncoding can coexist
        val encoding = Encoding(
            prefixEncoding = listOf(Encoding(style = EncodingStyle.FORM)),
            itemEncoding = Encoding(style = EncodingStyle.PIPE_DELIMITED)
        )
        val json = compactJson.encodeToString(encoding)

        val expected = """{"prefixEncoding":[{"style":"form"}],"itemEncoding":{"style":"pipeDelimited"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should round-trip Encoding object`() {
        val encoding = Encoding(
            contentType = "application/json",
            style = EncodingStyle.FORM,
            explode = true,
            allowReserved = false
        )

        TestHelpers.testRoundTripWithoutValidation(Encoding.serializer(), encoding)
    }

    // MediaType Tests

    @Test
    fun `should serialize minimal MediaType`() {
        val schema = Schema(buildJsonObject { put("type", "string") })
        val mediaType = MediaType(schema = schema)
        val json = compactJson.encodeToString(mediaType)

        assertEquals("""{"schema":{"type":"string"}}""", json)
    }

    @Test
    fun `should serialize MediaType with description`() {
        val schema = Schema(buildJsonObject { put("type", "object") })
        val mediaType = MediaType(
            description = "A JSON object",
            schema = schema
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"description":"A JSON object","schema":{"type":"object"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize MediaType with schema and itemSchema`() {
        val schema = Schema(buildJsonObject { put("type", "array") })
        val itemSchema = Schema(buildJsonObject { put("type", "string") })
        val mediaType = MediaType(
            schema = schema,
            itemSchema = itemSchema
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"schema":{"type":"array"},"itemSchema":{"type":"string"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize MediaType with encoding`() {
        val schema = Schema(buildJsonObject { put("type", "object") })
        val mediaType = MediaType(
            schema = schema,
            encoding = mapOf(
                "profileImage" to Encoding(
                    contentType = "image/png, image/jpeg"
                ),
                "children" to Encoding(
                    style = EncodingStyle.FORM,
                    explode = true
                )
            )
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"schema":{"type":"object"},"encoding":{"profileImage":{"contentType":"image/png, image/jpeg"},"children":{"style":"form","explode":true}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize MediaType with example`() {
        val schema = Schema(buildJsonObject { put("type", "string") })
        val mediaType = MediaType(
            schema = schema,
            example = JsonPrimitive("example value")
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"schema":{"type":"string"},"example":"example value"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize MediaType with examples`() {
        val schema = Schema(buildJsonObject { put("type", "object") })
        val mediaType = MediaType(
            schema = schema,
            examples = mapOf(
                "user1" to Example(
                    summary = "First user",
                    value = buildJsonObject {
                        put("name", "John")
                        put("age", 30)
                    }
                ),
                "user2" to Example(
                    summary = "Second user",
                    value = buildJsonObject {
                        put("name", "Jane")
                        put("age", 25)
                    }
                )
            )
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"schema":{"type":"object"},"examples":{"user1":{"summary":"First user","value":{"name":"John","age":30}},"user2":{"summary":"Second user","value":{"name":"Jane","age":25}}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize MediaType with prefixEncoding`() {
        val schema = Schema(buildJsonObject { put("type", "array") })
        val mediaType = MediaType(
            schema = schema,
            prefixEncoding = listOf(
                Encoding(contentType = "application/json"),
                Encoding(style = EncodingStyle.FORM)
            )
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"schema":{"type":"array"},"prefixEncoding":[{"contentType":"application/json"},{"style":"form"}]}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize MediaType with itemEncoding`() {
        val schema = Schema(buildJsonObject { put("type", "array") })
        val mediaType = MediaType(
            schema = schema,
            itemEncoding = Encoding(
                contentType = "application/json",
                style = EncodingStyle.DEEP_OBJECT
            )
        )
        val json = compactJson.encodeToString(mediaType)

        val expected = """{"schema":{"type":"array"},"itemEncoding":{"contentType":"application/json","style":"deepObject"}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should reject MediaType with both encoding and prefixEncoding`() {
        val schema = Schema(buildJsonObject { put("type", "object") })
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MediaType(
                schema = schema,
                encoding = mapOf("field" to Encoding()),
                prefixEncoding = listOf(Encoding())
            )
        }

        assertEquals(
            "MediaType 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `should reject MediaType with both encoding and itemEncoding`() {
        val schema = Schema(buildJsonObject { put("type", "object") })
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MediaType(
                schema = schema,
                encoding = mapOf("field" to Encoding()),
                itemEncoding = Encoding()
            )
        }

        assertEquals(
            "MediaType 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `should reject MediaType with both example and examples`() {
        val schema = Schema(buildJsonObject { put("type", "string") })
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MediaType(
                schema = schema,
                example = JsonPrimitive("test"),
                examples = mapOf("ex1" to Example(value = JsonPrimitive("test")))
            )
        }

        assertEquals(
            "MediaType 'example' and 'examples' are mutually exclusive",
            exception.message
        )
    }

    @Test
    fun `should round-trip MediaType object`() {
        val schema = Schema(buildJsonObject {
            put("type", "object")
            put("required", buildJsonObject {
                put("name", "string")
            })
        })
        val mediaType = MediaType(
            description = "User object",
            schema = schema,
            example = buildJsonObject {
                put("name", "Alice")
                put("email", "alice@example.com")
            }
        )

        TestHelpers.testRoundTripWithoutValidation(MediaType.serializer(), mediaType)
    }

    // Content Tests

    @Test
    fun `should serialize Content map`() {
        val content: Content = mapOf(
            "application/json" to MediaType(
                schema = Schema(buildJsonObject { put("type", "object") })
            ),
            "application/xml" to MediaType(
                schema = Schema(buildJsonObject { put("type", "object") })
            )
        )

        val json = compactJson.encodeToString(content)
        val expected = """{"application/json":{"schema":{"type":"object"}},"application/xml":{"schema":{"type":"object"}}}"""
        assertEquals(expected, json)
    }

    // TODO: Re-enable when Referenceable support is added
    // @Test
    // fun `should serialize Content with MediaType reference`() {
    //     val content: Content = mapOf(
    //         "application/json" to Reference(ref = "#/components/mediaTypes/JsonMediaType"),
    //         "text/plain" to MediaType(
    //             schema = Schema(buildJsonObject { put("type", "string") })
    //         )
    //     )
    //
    //     val json = compactJson.encodeToString(content)
    //     val expected = """{"application/json":{"${'$'}ref":"#/components/mediaTypes/JsonMediaType"},"text/plain":{"schema":{"type":"string"}}}"""
    //     assertEquals(expected, json)
    // }

    @Test
    fun `should serialize complex Content with encodings and examples`() {
        val content: Content = mapOf(
            "multipart/form-data" to MediaType(
                schema = Schema(buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("file", buildJsonObject {
                            put("type", "string")
                            put("format", "binary")
                        })
                    })
                }),
                encoding = mapOf(
                    "file" to Encoding(
                        contentType = "image/png, image/jpeg",
                        allowReserved = false
                    )
                )
            )
        )

        val json = compactJson.encodeToString(content)
        val expected = """{"multipart/form-data":{"schema":{"type":"object","properties":{"file":{"type":"string","format":"binary"}}},"encoding":{"file":{"contentType":"image/png, image/jpeg","allowReserved":false}}}}"""
        assertEquals(expected, json)
    }
}
