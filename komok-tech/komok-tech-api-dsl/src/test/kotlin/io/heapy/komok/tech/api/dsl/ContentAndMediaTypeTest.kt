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

    // ============================================
    // Encoding DSL Tests
    // ============================================

    @Test
    fun `encoding DSL should create with contentType only`() {
        val result = encoding {
            contentType = "application/json"
        }

        assertEquals(
            Encoding(contentType = "application/json"),
            result
        )
    }

    @Test
    fun `encoding DSL should create with all properties`() {
        val result = encoding {
            contentType = "application/json"
            style = EncodingStyle.FORM
            explode = true
            allowReserved = false
        }

        assertEquals(
            Encoding(
                contentType = "application/json",
                style = EncodingStyle.FORM,
                explode = true,
                allowReserved = false
            ),
            result
        )
    }

    @Test
    fun `encoding DSL should support all style types`() {
        val styles = listOf(
            EncodingStyle.FORM,
            EncodingStyle.SPACE_DELIMITED,
            EncodingStyle.PIPE_DELIMITED,
            EncodingStyle.DEEP_OBJECT
        )

        styles.forEach { encodingStyle ->
            val result = encoding {
                style = encodingStyle
            }
            assertEquals(encodingStyle, result.style)
        }
    }

    @Test
    fun `encoding DSL should support nested encoding map`() {
        val result = encoding {
            contentType = "application/json"
            encoding {
                "address" to {
                    style = EncodingStyle.DEEP_OBJECT
                }
                "tags" to {
                    style = EncodingStyle.SPACE_DELIMITED
                    explode = false
                }
            }
        }

        assertEquals(
            Encoding(
                contentType = "application/json",
                encoding = mapOf(
                    "address" to Encoding(style = EncodingStyle.DEEP_OBJECT),
                    "tags" to Encoding(style = EncodingStyle.SPACE_DELIMITED, explode = false)
                )
            ),
            result
        )
    }

    @Test
    fun `encoding DSL should support nested prefixEncoding`() {
        val result = encoding {
            contentType = "application/json"
            prefixEncoding {
                encoding {
                    style = EncodingStyle.FORM
                }
                encoding {
                    style = EncodingStyle.PIPE_DELIMITED
                }
            }
        }

        assertEquals(
            Encoding(
                contentType = "application/json",
                prefixEncoding = listOf(
                    Encoding(style = EncodingStyle.FORM),
                    Encoding(style = EncodingStyle.PIPE_DELIMITED)
                )
            ),
            result
        )
    }

    @Test
    fun `encoding DSL should support nested itemEncoding`() {
        val result = encoding {
            contentType = "application/json"
            itemEncoding {
                style = EncodingStyle.SPACE_DELIMITED
                explode = true
            }
        }

        assertEquals(
            Encoding(
                contentType = "application/json",
                itemEncoding = Encoding(
                    style = EncodingStyle.SPACE_DELIMITED,
                    explode = true
                )
            ),
            result
        )
    }

    @Test
    fun `encoding DSL should fail when encoding and prefixEncoding both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            encoding {
                encoding {
                    "field" to { style = EncodingStyle.FORM }
                }
                prefixEncoding {
                    encoding { style = EncodingStyle.FORM }
                }
            }
        }

        assertEquals(
            "Encoding 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `encoding DSL should fail when encoding and itemEncoding both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            encoding {
                encoding {
                    "field" to { style = EncodingStyle.FORM }
                }
                itemEncoding {
                    style = EncodingStyle.FORM
                }
            }
        }

        assertEquals(
            "Encoding 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `encoding DSL should allow prefixEncoding and itemEncoding together`() {
        val result = encoding {
            prefixEncoding {
                encoding { style = EncodingStyle.FORM }
            }
            itemEncoding {
                style = EncodingStyle.PIPE_DELIMITED
            }
        }

        assertEquals(
            Encoding(
                prefixEncoding = listOf(Encoding(style = EncodingStyle.FORM)),
                itemEncoding = Encoding(style = EncodingStyle.PIPE_DELIMITED)
            ),
            result
        )
    }

    @Test
    fun `encoding DSL should serialize correctly`() {
        val result = encoding {
            contentType = "image/png"
            style = EncodingStyle.FORM
            explode = true
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"contentType":"image/png","style":"form","explode":true}""",
            json
        )
    }

    @Test
    fun `encoding DSL should round-trip correctly`() {
        val result = encoding {
            contentType = "application/json"
            style = EncodingStyle.DEEP_OBJECT
            explode = true
            allowReserved = false
        }

        TestHelpers.testRoundTripWithoutValidation(Encoding.serializer(), result)
    }

    // ============================================
    // Encodings DSL Tests (Map builder)
    // ============================================

    @Test
    fun `encodings DSL should create map of encodings`() {
        val result = encodings {
            "profileImage" to {
                contentType = "image/png, image/jpeg"
            }
            "address" to {
                style = EncodingStyle.DEEP_OBJECT
                explode = true
            }
        }

        assertEquals(
            mapOf(
                "profileImage" to Encoding(contentType = "image/png, image/jpeg"),
                "address" to Encoding(style = EncodingStyle.DEEP_OBJECT, explode = true)
            ),
            result
        )
    }

    @Test
    fun `encodings DSL should create empty map`() {
        val result = encodings {}

        assertEquals(emptyMap<String, Encoding>(), result)
    }

    @Test
    fun `encodings DSL should accept pre-built encodings`() {
        val preBuilt = Encoding(contentType = "text/plain", style = EncodingStyle.FORM)

        val result = encodings {
            "description" to preBuilt
            "tags" to {
                style = EncodingStyle.SPACE_DELIMITED
            }
        }

        assertEquals(
            mapOf(
                "description" to Encoding(contentType = "text/plain", style = EncodingStyle.FORM),
                "tags" to Encoding(style = EncodingStyle.SPACE_DELIMITED)
            ),
            result
        )
    }

    // ============================================
    // PrefixEncodings DSL Tests (List builder)
    // ============================================

    @Test
    fun `prefixEncodings DSL should create list of encodings`() {
        val result = prefixEncodings {
            encoding {
                contentType = "application/json"
            }
            encoding {
                style = EncodingStyle.FORM
            }
        }

        assertEquals(
            listOf(
                Encoding(contentType = "application/json"),
                Encoding(style = EncodingStyle.FORM)
            ),
            result
        )
    }

    @Test
    fun `prefixEncodings DSL should create empty list`() {
        val result = prefixEncodings {}

        assertEquals(emptyList<Encoding>(), result)
    }

    @Test
    fun `prefixEncodings DSL should accept pre-built encodings`() {
        val preBuilt = Encoding(contentType = "text/xml")

        val result = prefixEncodings {
            encoding(preBuilt)
            encoding {
                style = EncodingStyle.PIPE_DELIMITED
            }
        }

        assertEquals(
            listOf(
                Encoding(contentType = "text/xml"),
                Encoding(style = EncodingStyle.PIPE_DELIMITED)
            ),
            result
        )
    }

    // ============================================
    // MediaType DSL with Encoding Tests
    // ============================================

    @Test
    fun `mediaType DSL should support nested encoding DSL`() {
        val result = mediaType {
            schema {
                type = "object"
            }
            encoding {
                "profileImage" to {
                    contentType = "image/png, image/jpeg"
                }
                "children" to {
                    style = EncodingStyle.FORM
                    explode = true
                }
            }
        }

        assertEquals(
            MediaType(
                schema = Schema(buildJsonObject { put("type", "object") }),
                encoding = mapOf(
                    "profileImage" to Encoding(contentType = "image/png, image/jpeg"),
                    "children" to Encoding(style = EncodingStyle.FORM, explode = true)
                )
            ),
            result
        )
    }

    @Test
    fun `mediaType DSL should support nested prefixEncoding DSL`() {
        val result = mediaType {
            schema {
                type = "array"
            }
            prefixEncoding {
                encoding {
                    contentType = "application/json"
                }
                encoding {
                    style = EncodingStyle.FORM
                }
            }
        }

        assertEquals(
            MediaType(
                schema = Schema(buildJsonObject { put("type", "array") }),
                prefixEncoding = listOf(
                    Encoding(contentType = "application/json"),
                    Encoding(style = EncodingStyle.FORM)
                )
            ),
            result
        )
    }

    @Test
    fun `mediaType DSL should support nested itemEncoding DSL`() {
        val result = mediaType {
            schema {
                type = "array"
            }
            itemEncoding {
                contentType = "application/json"
                style = EncodingStyle.DEEP_OBJECT
            }
        }

        assertEquals(
            MediaType(
                schema = Schema(buildJsonObject { put("type", "array") }),
                itemEncoding = Encoding(
                    contentType = "application/json",
                    style = EncodingStyle.DEEP_OBJECT
                )
            ),
            result
        )
    }

    @Test
    fun `mediaType DSL should support deeply nested encoding`() {
        val result = mediaType {
            schema {
                type = "object"
            }
            encoding {
                "data" to {
                    contentType = "application/json"
                    encoding {
                        "nested" to {
                            style = EncodingStyle.DEEP_OBJECT
                        }
                    }
                }
            }
        }

        assertEquals(
            MediaType(
                schema = Schema(buildJsonObject { put("type", "object") }),
                encoding = mapOf(
                    "data" to Encoding(
                        contentType = "application/json",
                        encoding = mapOf(
                            "nested" to Encoding(style = EncodingStyle.DEEP_OBJECT)
                        )
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `mediaType DSL should fail when encoding and prefixEncoding both set`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            mediaType {
                schema {
                    type = "object"
                }
                encoding {
                    "field" to { style = EncodingStyle.FORM }
                }
                prefixEncoding {
                    encoding { style = EncodingStyle.FORM }
                }
            }
        }

        assertEquals(
            "MediaType 'encoding' is mutually exclusive with 'prefixEncoding' and 'itemEncoding'",
            exception.message
        )
    }

    @Test
    fun `mediaType DSL should serialize encoding correctly`() {
        val result = mediaType {
            schema {
                type = "object"
            }
            encoding {
                "file" to {
                    contentType = "application/octet-stream"
                }
            }
        }
        val json = compactJson.encodeToString(result)

        val expected = """{"schema":{"type":"object"},"encoding":{"file":{"contentType":"application/octet-stream"}}}"""
        assertEquals(expected, json)
    }

    // ============================================
    // Content DSL Tests
    // ============================================

    @Test
    fun `content DSL should create content map`() {
        val result = content {
            "application/json" to {
                schema {
                    type = "object"
                }
            }
            "text/plain" to {
                schema {
                    type = "string"
                }
            }
        }

        assertEquals(
            mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") })
                ),
                "text/plain" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "string") })
                )
            ),
            result
        )
    }

    @Test
    fun `content DSL should create empty map`() {
        val result = content {}

        assertEquals(emptyMap<String, MediaType>(), result)
    }

    @Test
    fun `content DSL should accept pre-built media types`() {
        val preBuilt = MediaType(
            description = "Pre-built media type",
            schema = Schema(buildJsonObject { put("type", "number") })
        )

        val result = content {
            "application/json" to preBuilt
            "text/plain" to {
                schema {
                    type = "string"
                }
            }
        }

        assertEquals(
            mapOf(
                "application/json" to MediaType(
                    description = "Pre-built media type",
                    schema = Schema(buildJsonObject { put("type", "number") })
                ),
                "text/plain" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "string") })
                )
            ),
            result
        )
    }

    @Test
    fun `content DSL should support nested encoding`() {
        val result = content {
            "multipart/form-data" to {
                schema {
                    type = "object"
                }
                encoding {
                    "file" to {
                        contentType = "image/png, image/jpeg"
                        allowReserved = false
                    }
                }
            }
        }

        assertEquals(
            mapOf(
                "multipart/form-data" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") }),
                    encoding = mapOf(
                        "file" to Encoding(
                            contentType = "image/png, image/jpeg",
                            allowReserved = false
                        )
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `content DSL should support examples with encoding`() {
        val result = content {
            "application/json" to {
                schema {
                    type = "object"
                }
                examples {
                    "example1" to {
                        summary = "First example"
                        value = buildJsonObject {
                            put("name", "John")
                        }
                    }
                }
                encoding {
                    "data" to {
                        style = EncodingStyle.DEEP_OBJECT
                    }
                }
            }
        }

        assertEquals(
            mapOf(
                "application/json" to MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") }),
                    examples = mapOf(
                        "example1" to Example(
                            summary = "First example",
                            value = buildJsonObject { put("name", "John") }
                        )
                    ),
                    encoding = mapOf(
                        "data" to Encoding(style = EncodingStyle.DEEP_OBJECT)
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `content DSL should serialize correctly`() {
        val result = content {
            "application/json" to {
                schema {
                    type = "object"
                }
            }
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"application/json":{"schema":{"type":"object"}}}""",
            json
        )
    }

    @Test
    fun `content DSL complex multipart form data example`() {
        val result = content {
            "multipart/form-data" to {
                schema {
                    type = "object"
                    properties {
                        "orderId" to stringSchema()
                        "userId" to stringSchema()
                        "profileImage" to schema {
                            type = "string"
                            format = "binary"
                        }
                        "address" to objectSchema {
                            properties {
                                "street" to stringSchema()
                                "city" to stringSchema()
                            }
                        }
                    }
                }
                encoding {
                    "profileImage" to {
                        contentType = "image/png, image/jpeg"
                    }
                    "address" to {
                        style = EncodingStyle.DEEP_OBJECT
                        explode = true
                    }
                }
            }
        }

        // Verify structure
        val mediaType = result["multipart/form-data"]!!
        val encodingMap = mediaType.encoding!!
        assertEquals(2, encodingMap.size)
        assertEquals("image/png, image/jpeg", encodingMap["profileImage"]?.contentType)
        assertEquals(EncodingStyle.DEEP_OBJECT, encodingMap["address"]?.style)
        assertEquals(true, encodingMap["address"]?.explode)
    }
}
