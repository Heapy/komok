package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

/**
 * DSL builder for [ServerVariable] object.
 *
 * The builder enforces fail-fast validation:
 * - `default` is required
 * - `enum` must contain at least one value if provided
 *
 * Example usage:
 * ```kotlin
 * val serverVariable = serverVariable {
 *     default = "v1"
 *     enum = listOf("v1", "v2", "v3")
 *     description = "API version"
 * }
 * ```
 */
class ServerVariableBuilder {
    var default: String? = null
    var enum: List<String>? = null
    var description: String? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Sets the enum values from varargs.
     */
    fun enum(vararg values: String) {
        enum = values.toList()
    }

    fun build(): ServerVariable {
        val defaultValue = requireNotNull(default) {
            "ServerVariable default is required"
        }
        if (enum != null) {
            require(enum!!.isNotEmpty()) {
                "ServerVariable enum must contain at least one value"
            }
        }
        return ServerVariable(
            default = defaultValue,
            enum = enum,
            description = description,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [ServerVariable] object using DSL syntax.
 *
 * @param block configuration block for the server variable
 * @return configured ServerVariable object
 * @throws IllegalArgumentException if default is not provided
 * @throws IllegalArgumentException if enum is empty
 */
inline fun serverVariable(block: ServerVariableBuilder.() -> Unit): ServerVariable {
    return ServerVariableBuilder().apply(block).build()
}

/**
 * DSL builder for server variables map.
 *
 * Example usage:
 * ```kotlin
 * val variables = serverVariables {
 *     "environment" to {
 *         default = "api"
 *         enum("api", "staging", "dev")
 *     }
 *     "port" to {
 *         default = "8443"
 *         description = "Server port"
 *     }
 * }
 * ```
 */
class ServerVariablesBuilder {
    @PublishedApi
    internal val variables = mutableMapOf<String, ServerVariable>()

    /**
     * Adds a server variable using DSL syntax.
     */
    inline infix fun String.to(block: ServerVariableBuilder.() -> Unit) {
        variables[this] = serverVariable(block)
    }

    /**
     * Adds a pre-built server variable.
     */
    infix fun String.to(variable: ServerVariable) {
        variables[this] = variable
    }

    fun build(): Map<String, ServerVariable> = variables.toMap()
}

/**
 * Creates a map of server variables using DSL syntax.
 *
 * @param block configuration block for the server variables
 * @return map of server variable names to ServerVariable objects
 */
inline fun serverVariables(block: ServerVariablesBuilder.() -> Unit): Map<String, ServerVariable> {
    return ServerVariablesBuilder().apply(block).build()
}

/**
 * DSL builder for [Server] object.
 *
 * The builder enforces fail-fast validation:
 * - `url` is required
 *
 * Supports nested DSL for server variables:
 * ```kotlin
 * val server = server {
 *     url = "https://{environment}.example.com:{port}"
 *     description = "API server"
 *     variables {
 *         "environment" to {
 *             default = "api"
 *             enum("api", "staging")
 *         }
 *         "port" to {
 *             default = "8443"
 *         }
 *     }
 * }
 * ```
 */
class ServerBuilder {
    var url: String? = null
    var description: String? = null
    var name: String? = null
    var variables: Map<String, ServerVariable>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures server variables using DSL syntax.
     */
    inline fun variables(block: ServerVariablesBuilder.() -> Unit) {
        variables = serverVariables(block)
    }

    fun build(): Server {
        val serverUrl = requireNotNull(url) {
            "Server url is required"
        }
        return Server(
            url = serverUrl,
            description = description,
            name = name,
            variables = variables,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [Server] object using DSL syntax.
 *
 * @param block configuration block for the server
 * @return configured Server object
 * @throws IllegalArgumentException if url is not provided
 */
inline fun server(block: ServerBuilder.() -> Unit): Server {
    return ServerBuilder().apply(block).build()
}

/**
 * DSL builder for creating a list of [Server] objects.
 *
 * Example usage:
 * ```kotlin
 * val servers = servers {
 *     server {
 *         url = "https://api.example.com"
 *         description = "Production server"
 *     }
 *     server {
 *         url = "https://staging.example.com"
 *         description = "Staging server"
 *     }
 * }
 * ```
 */
class ServersBuilder {
    @PublishedApi
    internal val servers = mutableListOf<Server>()

    /**
     * Adds a server using DSL syntax.
     */
    inline fun server(block: ServerBuilder.() -> Unit) {
        servers.add(io.heapy.komok.tech.api.dsl.server(block))
    }

    /**
     * Adds a pre-built server.
     */
    fun server(server: Server) {
        servers.add(server)
    }

    fun build(): List<Server> = servers.toList()
}

/**
 * Creates a list of [Server] objects using DSL syntax.
 *
 * @param block configuration block for the servers
 * @return list of configured Server objects
 */
inline fun servers(block: ServersBuilder.() -> Unit): List<Server> {
    return ServersBuilder().apply(block).build()
}
