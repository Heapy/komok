package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerDslTest {

    // ServerVariable DSL Tests

    @Test
    fun `serverVariable DSL should create with default only`() {
        val result = serverVariable {
            default = "v1"
        }

        assertEquals(
            ServerVariable(default = "v1"),
            result
        )
    }

    @Test
    fun `serverVariable DSL should create with all properties`() {
        val result = serverVariable {
            default = "production"
            enum = listOf("production", "staging", "development")
            description = "Environment name"
            extensions = mapOf("x-internal" to JsonPrimitive(true))
        }

        assertEquals(
            ServerVariable(
                default = "production",
                enum = listOf("production", "staging", "development"),
                description = "Environment name",
                extensions = mapOf("x-internal" to JsonPrimitive(true))
            ),
            result
        )
    }

    @Test
    fun `serverVariable DSL should support enum varargs`() {
        val result = serverVariable {
            default = "v2"
            enum("v1", "v2", "v3")
        }

        assertEquals(
            ServerVariable(
                default = "v2",
                enum = listOf("v1", "v2", "v3")
            ),
            result
        )
    }

    @Test
    fun `serverVariable DSL should fail when default is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            serverVariable {
                enum = listOf("v1", "v2")
            }
        }

        assertEquals("ServerVariable default is required", exception.message)
    }

    @Test
    fun `serverVariable DSL should fail when enum is empty`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            serverVariable {
                default = "v1"
                enum = emptyList()
            }
        }

        assertEquals("ServerVariable enum must contain at least one value", exception.message)
    }

    @Test
    fun `serverVariable DSL should serialize correctly`() {
        val result = serverVariable {
            default = "8443"
            description = "Port number"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"default":"8443","description":"Port number"}""",
            json
        )
    }

    @Test
    fun `serverVariable DSL should round-trip correctly`() {
        val result = serverVariable {
            default = "api"
            enum("api", "staging")
            description = "Environment"
        }

        TestHelpers.testRoundTripWithoutValidation(ServerVariable.serializer(), result)
    }

    // ServerVariables DSL Tests

    @Test
    fun `serverVariables DSL should create map of variables`() {
        val result = serverVariables {
            "environment" to {
                default = "api"
                enum("api", "staging")
            }
            "port" to {
                default = "8443"
            }
        }

        assertEquals(
            mapOf(
                "environment" to ServerVariable(
                    default = "api",
                    enum = listOf("api", "staging")
                ),
                "port" to ServerVariable(default = "8443")
            ),
            result
        )
    }

    @Test
    fun `serverVariables DSL should create empty map`() {
        val result = serverVariables {}

        assertEquals(emptyMap<String, ServerVariable>(), result)
    }

    @Test
    fun `serverVariables DSL should accept pre-built variables`() {
        val preBuilt = ServerVariable(default = "v1", description = "API version")

        val result = serverVariables {
            "version" to preBuilt
            "port" to {
                default = "443"
            }
        }

        assertEquals(
            mapOf(
                "version" to ServerVariable(default = "v1", description = "API version"),
                "port" to ServerVariable(default = "443")
            ),
            result
        )
    }

    // Server DSL Tests

    @Test
    fun `server DSL should create with url only`() {
        val result = server {
            url = "https://api.example.com"
        }

        assertEquals(
            Server(url = "https://api.example.com"),
            result
        )
    }

    @Test
    fun `server DSL should create with all properties`() {
        val result = server {
            url = "https://api.example.com/v1"
            description = "Production server"
            name = "production"
            extensions = mapOf("x-region" to JsonPrimitive("us-east-1"))
        }

        assertEquals(
            Server(
                url = "https://api.example.com/v1",
                description = "Production server",
                name = "production",
                extensions = mapOf("x-region" to JsonPrimitive("us-east-1"))
            ),
            result
        )
    }

    @Test
    fun `server DSL should support nested variables DSL`() {
        val result = server {
            url = "https://{environment}.example.com:{port}"
            description = "API server"
            variables {
                "environment" to {
                    default = "api"
                    enum("api", "staging")
                }
                "port" to {
                    default = "8443"
                    description = "Server port"
                }
            }
        }

        assertEquals(
            Server(
                url = "https://{environment}.example.com:{port}",
                description = "API server",
                variables = mapOf(
                    "environment" to ServerVariable(
                        default = "api",
                        enum = listOf("api", "staging")
                    ),
                    "port" to ServerVariable(
                        default = "8443",
                        description = "Server port"
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `server DSL should fail when url is not provided`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            server {
                description = "Missing URL server"
            }
        }

        assertEquals("Server url is required", exception.message)
    }

    @Test
    fun `server DSL should fail when nested variable has no default`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            server {
                url = "https://{env}.example.com"
                variables {
                    "env" to {
                        description = "Missing default"
                    }
                }
            }
        }

        assertEquals("ServerVariable default is required", exception.message)
    }

    @Test
    fun `server DSL should serialize correctly`() {
        val result = server {
            url = "https://api.example.com"
            description = "Main server"
        }
        val json = compactJson.encodeToString(result)

        assertEquals(
            """{"url":"https://api.example.com","description":"Main server"}""",
            json
        )
    }

    @Test
    fun `server DSL should round-trip correctly`() {
        val result = server {
            url = "https://{env}.example.com"
            description = "Server with variables"
            variables {
                "env" to {
                    default = "api"
                    enum("api", "staging", "dev")
                }
            }
        }

        TestHelpers.testRoundTripWithoutValidation(Server.serializer(), result)
    }

    @Test
    fun `server DSL should support relative url`() {
        val result = server {
            url = "/api/v1"
            description = "Relative API path"
        }

        assertEquals(
            Server(url = "/api/v1", description = "Relative API path"),
            result
        )
    }

    // Servers (list) DSL Tests

    @Test
    fun `servers DSL should create list of servers`() {
        val result = servers {
            server {
                url = "https://api.example.com"
                description = "Production"
            }
            server {
                url = "https://staging.example.com"
                description = "Staging"
            }
        }

        assertEquals(
            listOf(
                Server(url = "https://api.example.com", description = "Production"),
                Server(url = "https://staging.example.com", description = "Staging")
            ),
            result
        )
    }

    @Test
    fun `servers DSL should create empty list`() {
        val result = servers {}

        assertEquals(emptyList<Server>(), result)
    }

    @Test
    fun `servers DSL should accept pre-built servers`() {
        val preBuilt = Server(url = "https://api.example.com", name = "main")

        val result = servers {
            server(preBuilt)
            server {
                url = "https://backup.example.com"
            }
        }

        assertEquals(
            listOf(
                Server(url = "https://api.example.com", name = "main"),
                Server(url = "https://backup.example.com")
            ),
            result
        )
    }

    @Test
    fun `servers DSL should fail if any server is missing url`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            servers {
                server {
                    url = "https://valid.example.com"
                }
                server {
                    description = "Missing URL"
                }
            }
        }

        assertEquals("Server url is required", exception.message)
    }

    @Test
    fun `servers DSL should support nested variables in each server`() {
        val result = servers {
            server {
                url = "https://{env}.example.com"
                variables {
                    "env" to {
                        default = "prod"
                    }
                }
            }
        }

        assertEquals(
            listOf(
                Server(
                    url = "https://{env}.example.com",
                    variables = mapOf("env" to ServerVariable(default = "prod"))
                )
            ),
            result
        )
    }
}
