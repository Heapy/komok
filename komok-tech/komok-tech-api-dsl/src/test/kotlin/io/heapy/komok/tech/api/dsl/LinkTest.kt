package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LinkTest {

    // Basic Link Tests

    @Test
    fun `should serialize Link with operationRef`() {
        val link = Link(
            operationRef = "#/paths/~12.0~1repositories~1{username}/get"
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("operationRef"))
        assert(json.contains("#/paths/~12.0~1repositories~1{username}/get"))
    }

    @Test
    fun `should serialize Link with operationId`() {
        val link = Link(
            operationId = "getUserRepositories"
        )
        val json = compactJson.encodeToString(link)

        val expected = """{"operationId":"getUserRepositories"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `should serialize Link with description`() {
        val link = Link(
            operationId = "getUser",
            description = "The userId value returned in the response can be used as the user parameter in GET /users/{user}"
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("description"))
        assert(json.contains("userId"))
    }

    // Parameters Tests

    @Test
    fun `should serialize Link with parameters`() {
        val link = Link(
            operationId = "getUserAddress",
            parameters = mapOf(
                "userId" to "\$response.body#/id"
            )
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("parameters"))
        assert(json.contains("userId"))
        assert(json.contains("\$response.body#/id"))
    }

    @Test
    fun `should serialize Link with multiple parameters`() {
        val link = Link(
            operationId = "getResource",
            parameters = mapOf(
                "id" to "\$response.body#/id",
                "status" to "\$response.body#/status",
                "userId" to "\$request.path.userId"
            )
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("id"))
        assert(json.contains("status"))
        assert(json.contains("userId"))
    }

    // RequestBody Tests

    @Test
    fun `should serialize Link with requestBody as string`() {
        val link = Link(
            operationId = "createUser",
            requestBody = JsonPrimitive("\$response.body#/user")
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("requestBody"))
        assert(json.contains("\$response.body#/user"))
    }

    @Test
    fun `should serialize Link with requestBody as object`() {
        val link = Link(
            operationId = "updateUser",
            requestBody = buildJsonObject {
                put("userId", "\$response.body#/id")
                put("username", "\$response.body#/username")
            }
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("requestBody"))
        assert(json.contains("userId"))
        assert(json.contains("username"))
    }

    // Server Tests

    @Test
    fun `should serialize Link with server`() {
        val link = Link(
            operationId = "getResource",
            server = Server(
                url = "https://api.example.com/v2",
                description = "V2 API server"
            )
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("server"))
        assert(json.contains("https://api.example.com/v2"))
    }

    // Specification Extensions Tests

    @Test
    fun `should serialize Link with specification extensions`() {
        val link = Link(
            operationId = "getUser",
            extensions = mapOf(
                "x-internal-id" to JsonPrimitive("link-123"),
                "x-priority" to JsonPrimitive(1)
            )
        )
        val json = compactJson.encodeToString(link)

        assert(json.contains("x-internal-id"))
        assert(json.contains("link-123"))
    }

    // Validation Tests

    @Test
    fun `should reject Link without operationRef or operationId`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Link(
                description = "Invalid link"
            )
        }

        assertEquals(
            "Link must have exactly one of 'operationRef' or 'operationId' specified",
            exception.message
        )
    }

    @Test
    fun `should reject Link with both operationRef and operationId`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Link(
                operationRef = "#/paths/~1users/get",
                operationId = "getUsers"
            )
        }

        assertEquals(
            "Link must have exactly one of 'operationRef' or 'operationId' specified",
            exception.message
        )
    }

    // Deserialization Tests

    @Test
    fun `should deserialize Link with operationId`() {
        val json = """{"operationId":"getUserByName","parameters":{"username":"${'$'}response.body#/username"}}"""
        val link = compactJson.decodeFromString<Link>(json)

        assertEquals("getUserByName", link.operationId)
        assertEquals("\$response.body#/username", link.parameters?.get("username"))
    }

    @Test
    fun `should deserialize Link with operationRef`() {
        val json = """{"operationRef":"#/paths/~1users~1{id}/get"}"""
        val link = compactJson.decodeFromString<Link>(json)

        assertEquals("#/paths/~1users~1{id}/get", link.operationRef)
    }

    // Round-trip Tests

    @Test
    fun `should round-trip Link with operationId`() {
        val link = Link(
            operationId = "getUserRepositories",
            parameters = mapOf(
                "username" to "\$response.body#/username"
            ),
            description = "Link to user repositories"
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    @Test
    fun `should round-trip Link with operationRef`() {
        val link = Link(
            operationRef = "#/paths/~1users~1{id}/get",
            parameters = mapOf(
                "id" to "\$response.body#/id"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    @Test
    fun `should round-trip Link with all properties`() {
        val link = Link(
            operationId = "createResource",
            parameters = mapOf(
                "resourceId" to "\$response.body#/id",
                "userId" to "\$request.path.userId"
            ),
            requestBody = buildJsonObject {
                put("name", "\$response.body#/name")
                put("description", "\$response.body#/description")
            },
            description = "Creates a related resource",
            server = Server(
                url = "https://api.example.com"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    // Real-world Examples

    @Test
    fun `should serialize GitHub-style link`() {
        val link = Link(
            operationId = "getUserRepositories",
            parameters = mapOf(
                "username" to "\$response.body#/login"
            ),
            description = "Link to the user's repositories"
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    @Test
    fun `should serialize pagination link`() {
        val link = Link(
            operationRef = "#/paths/~1users/get",
            parameters = mapOf(
                "page" to "\$response.body#/page",
                "limit" to "\$request.query.limit"
            ),
            description = "Next page of results"
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    @Test
    fun `should serialize HATEOAS-style link`() {
        val link = Link(
            operationId = "updateOrder",
            requestBody = buildJsonObject {
                put("orderId", "\$response.body#/id")
                put("status", "shipped")
            },
            description = "Mark order as shipped"
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    @Test
    fun `should serialize link with runtime expression in parameter`() {
        val link = Link(
            operationId = "getOrderById",
            parameters = mapOf(
                "orderId" to "\$response.body#/orders/0/id",
                "customerId" to "\$request.header.X-Customer-Id"
            )
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }

    @Test
    fun `should serialize link with path substitution`() {
        val link = Link(
            operationRef = "#/paths/~1users~1{userId}~1orders~1{orderId}/get",
            parameters = mapOf(
                "userId" to "\$response.body#/userId",
                "orderId" to "\$response.body#/orderId"
            ),
            description = "Get specific order for a user"
        )

        TestHelpers.testRoundTripWithoutValidation(Link.serializer(), link)
    }
}
