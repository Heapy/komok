package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReferenceResolverTest {

    private fun minimalOpenAPI(components: Components) = OpenAPI(
        openapi = "3.2.0",
        info = Info(title = "Test", version = "1.0.0"),
        components = components,
    )

    // --- Successful resolution (one per component type) ---

    @Test
    fun `should resolve schema reference`() {
        val schema = Schema(buildJsonObject { put("type", "object") })
        val doc = minimalOpenAPI(Components(schemas = mapOf("Pet" to schema)))
        val resolver = ReferenceResolver(doc)

        assertEquals(schema, resolver.resolve("#/components/schemas/Pet"))
    }

    @Test
    fun `should resolve response reference`() {
        val response = Response(description = "OK")
        val doc = minimalOpenAPI(Components(responses = mapOf("OK" to response)))
        val resolver = ReferenceResolver(doc)

        assertEquals(response, resolver.resolve("#/components/responses/OK"))
    }

    @Test
    fun `should resolve parameter reference`() {
        val parameter = Parameter(
            name = "limit",
            location = ParameterLocation.QUERY,
            schema = Schema(buildJsonObject { put("type", "integer") }),
        )
        val doc = minimalOpenAPI(Components(parameters = mapOf("limit" to parameter)))
        val resolver = ReferenceResolver(doc)

        assertEquals(parameter, resolver.resolve("#/components/parameters/limit"))
    }

    @Test
    fun `should resolve example reference`() {
        val example = Example(summary = "A sample")
        val doc = minimalOpenAPI(Components(examples = mapOf("sample" to example)))
        val resolver = ReferenceResolver(doc)

        assertEquals(example, resolver.resolve("#/components/examples/sample"))
    }

    @Test
    fun `should resolve requestBody reference`() {
        val requestBody = RequestBody(
            content = mapOf(
                "application/json" to Direct(MediaType(
                    schema = Schema(buildJsonObject { put("type", "object") }),
                )),
            ),
        )
        val doc = minimalOpenAPI(Components(requestBodies = mapOf("Body" to requestBody)))
        val resolver = ReferenceResolver(doc)

        assertEquals(requestBody, resolver.resolve("#/components/requestBodies/Body"))
    }

    @Test
    fun `should resolve header reference`() {
        val header = Header(
            schema = Schema(buildJsonObject { put("type", "string") }),
        )
        val doc = minimalOpenAPI(Components(headers = mapOf("X-Custom" to header)))
        val resolver = ReferenceResolver(doc)

        assertEquals(header, resolver.resolve("#/components/headers/X-Custom"))
    }

    @Test
    fun `should resolve securityScheme reference`() {
        val scheme = SecurityScheme.http(scheme = "bearer")
        val doc = minimalOpenAPI(Components(securitySchemes = mapOf("bearerAuth" to scheme)))
        val resolver = ReferenceResolver(doc)

        assertEquals(scheme, resolver.resolve("#/components/securitySchemes/bearerAuth"))
    }

    @Test
    fun `should resolve link reference`() {
        val link = Link(operationId = "getUser")
        val doc = minimalOpenAPI(Components(links = mapOf("UserLink" to link)))
        val resolver = ReferenceResolver(doc)

        assertEquals(link, resolver.resolve("#/components/links/UserLink"))
    }

    @Test
    fun `should resolve callback reference`() {
        val callback = Callback(mapOf(
            "http://example.com" to PathItem(
                get = Operation(responses = responses("200" to Response(description = "OK"))),
            ),
        ))
        val doc = minimalOpenAPI(Components(callbacks = mapOf("onEvent" to Direct(callback))))
        val resolver = ReferenceResolver(doc)

        assertEquals(callback, resolver.resolve("#/components/callbacks/onEvent"))
    }

    @Test
    fun `should resolve pathItem reference`() {
        val pathItem = PathItem(
            get = Operation(responses = responses("200" to Response(description = "OK"))),
        )
        val doc = minimalOpenAPI(Components(pathItems = mapOf("common" to pathItem)))
        val resolver = ReferenceResolver(doc)

        assertEquals(pathItem, resolver.resolve("#/components/pathItems/common"))
    }

    @Test
    fun `should resolve mediaType reference`() {
        val mediaType = MediaType(
            schema = Schema(buildJsonObject { put("type", "string") }),
        )
        val doc = minimalOpenAPI(Components(mediaTypes = mapOf("jsonType" to Direct(mediaType))))
        val resolver = ReferenceResolver(doc)

        assertEquals(mediaType, resolver.resolve("#/components/mediaTypes/jsonType"))
    }

    // --- Chain resolution ---

    @Test
    fun `should resolve callback that is itself a reference`() {
        val callback = Callback(mapOf(
            "http://example.com" to PathItem(
                get = Operation(responses = responses("200" to Response(description = "OK"))),
            ),
        ))
        val doc = minimalOpenAPI(Components(callbacks = mapOf(
            "alias" to Reference(ref = "#/components/callbacks/real"),
            "real" to Direct(callback),
        )))
        val resolver = ReferenceResolver(doc)

        assertEquals(callback, resolver.resolve("#/components/callbacks/alias"))
    }

    @Test
    fun `should resolve mediaType that is itself a reference`() {
        val mediaType = MediaType(
            schema = Schema(buildJsonObject { put("type", "string") }),
        )
        val doc = minimalOpenAPI(Components(mediaTypes = mapOf(
            "alias" to Reference(ref = "#/components/mediaTypes/real"),
            "real" to Direct(mediaType),
        )))
        val resolver = ReferenceResolver(doc)

        assertEquals(mediaType, resolver.resolve("#/components/mediaTypes/alias"))
    }

    @Test
    fun `should resolve multi-step reference chain`() {
        val callback = Callback(mapOf(
            "http://example.com" to PathItem(
                get = Operation(responses = responses("200" to Response(description = "OK"))),
            ),
        ))
        val doc = minimalOpenAPI(Components(callbacks = mapOf(
            "a" to Reference(ref = "#/components/callbacks/b"),
            "b" to Reference(ref = "#/components/callbacks/c"),
            "c" to Direct(callback),
        )))
        val resolver = ReferenceResolver(doc)

        assertEquals(callback, resolver.resolve("#/components/callbacks/a"))
    }

    // --- resolveReferenceable ---

    @Test
    fun `should unwrap Direct value`() {
        val response = Response(description = "OK")
        val doc = minimalOpenAPI(Components())
        val resolver = ReferenceResolver(doc)

        assertEquals(response, resolver.resolveReferenceable(Direct(response)))
    }

    @Test
    fun `should resolve Reference via resolveReferenceable`() {
        val response = Response(description = "Found")
        val doc = minimalOpenAPI(Components(responses = mapOf("Found" to response)))
        val resolver = ReferenceResolver(doc)

        val ref: Referenceable<Response> = Reference(ref = "#/components/responses/Found")
        assertEquals(response, resolver.resolveReferenceable(ref))
    }

    // --- Convenience overload ---

    @Test
    fun `should resolve from Reference object`() {
        val schema = Schema(buildJsonObject { put("type", "string") })
        val doc = minimalOpenAPI(Components(schemas = mapOf("Name" to schema)))
        val resolver = ReferenceResolver(doc)

        val reference = Reference(ref = "#/components/schemas/Name")
        assertEquals(schema, resolver.resolve(reference))
    }

    // --- Error cases ---

    @Test
    fun `should throw for non-local reference`() {
        val doc = minimalOpenAPI(Components())
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("https://example.com/schemas/Pet")
        }
        assertEquals("https://example.com/schemas/Pet", ex.ref)
    }

    @Test
    fun `should throw for empty pointer`() {
        val doc = minimalOpenAPI(Components())
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/")
        }
        assertEquals("#/", ex.ref)
    }

    @Test
    fun `should throw for wrong segment count`() {
        val doc = minimalOpenAPI(Components())
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/components/schemas")
        }
        assertEquals("#/components/schemas", ex.ref)
    }

    @Test
    fun `should throw for non-components root`() {
        val doc = minimalOpenAPI(Components())
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/info/title/x")
        }
        assertEquals("#/info/title/x", ex.ref)
    }

    @Test
    fun `should throw when document has no components`() {
        val doc = OpenAPI(
            openapi = "3.2.0",
            info = Info(title = "Test", version = "1.0.0"),
            paths = mapOf("/" to PathItem()),
        )
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/components/schemas/Pet")
        }
        assertEquals("#/components/schemas/Pet", ex.ref)
    }

    @Test
    fun `should throw for unknown component type`() {
        val doc = minimalOpenAPI(Components())
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/components/foobar/X")
        }
        assertEquals("#/components/foobar/X", ex.ref)
    }

    @Test
    fun `should throw for missing component`() {
        val doc = minimalOpenAPI(Components(
            schemas = mapOf(
                "Pet" to Schema(buildJsonObject { put("type", "object") }),
            ),
        ))
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/components/schemas/NonExistent")
        }
        assertEquals("#/components/schemas/NonExistent", ex.ref)
    }

    @Test
    fun `should throw when chain depth exceeded`() {
        val doc = minimalOpenAPI(Components(callbacks = mapOf(
            "loop" to Reference(ref = "#/components/callbacks/loop"),
        )))
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/components/callbacks/loop")
        }
        assertEquals("#/components/callbacks/loop", ex.ref)
    }

    @Test
    fun `should throw for null component map`() {
        val doc = minimalOpenAPI(Components(schemas = null))
        val resolver = ReferenceResolver(doc)

        val ex = assertThrows(ReferenceResolutionException::class.java) {
            resolver.resolve("#/components/schemas/Pet")
        }
        assertEquals("#/components/schemas/Pet", ex.ref)
    }
}
