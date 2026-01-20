package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

// ============================================
// Referenceable Parameters DSL
// ============================================

/**
 * DSL builder for referenceable parameters list.
 *
 * Allows building a list of parameters that can be either inline definitions
 * or references to components.
 *
 * Example usage:
 * ```kotlin
 * val params = referenceableParameters {
 *     parameter {
 *         name = "userId"
 *         location = ParameterLocation.PATH
 *         required = true
 *         schema { type = "string" }
 *     }
 *     ref("#/components/parameters/PaginationLimit")
 * }
 * ```
 */
class ReferenceableParametersBuilder {
    @PublishedApi
    internal val parameters = mutableListOf<Referenceable<Parameter>>()

    /**
     * Adds an inline parameter using DSL syntax.
     */
    inline fun parameter(block: ParameterBuilder.() -> Unit) {
        parameters.add(Direct(io.heapy.komok.tech.api.dsl.parameter(block)))
    }

    /**
     * Adds a pre-built parameter as inline.
     */
    fun parameter(parameter: Parameter) {
        parameters.add(Direct(parameter))
    }

    /**
     * Adds a reference to a parameter component.
     */
    fun ref(ref: String, summary: String? = null, description: String? = null) {
        parameters.add(Reference(ref = ref, summary = summary, description = description))
    }

    /**
     * Adds a path parameter using convenience DSL.
     */
    inline fun pathParameter(
        name: String,
        description: String? = null,
        block: SchemaBuilder.() -> Unit = {},
    ) {
        parameters.add(Direct(io.heapy.komok.tech.api.dsl.pathParameter(name, description, block)))
    }

    /**
     * Adds a query parameter using convenience DSL.
     */
    inline fun queryParameter(
        name: String,
        description: String? = null,
        required: Boolean = false,
        block: SchemaBuilder.() -> Unit = {},
    ) {
        parameters.add(Direct(io.heapy.komok.tech.api.dsl.queryParameter(name, description, required, block)))
    }

    /**
     * Adds a header parameter using convenience DSL.
     */
    inline fun headerParameter(
        name: String,
        description: String? = null,
        required: Boolean = false,
        block: SchemaBuilder.() -> Unit = {},
    ) {
        parameters.add(Direct(io.heapy.komok.tech.api.dsl.headerParameter(name, description, required, block)))
    }

    /**
     * Adds a cookie parameter using convenience DSL.
     */
    inline fun cookieParameter(
        name: String,
        description: String? = null,
        required: Boolean = false,
        block: SchemaBuilder.() -> Unit = {},
    ) {
        parameters.add(Direct(io.heapy.komok.tech.api.dsl.cookieParameter(name, description, required, block)))
    }

    fun build(): List<Referenceable<Parameter>> = parameters.toList()
}

/**
 * Creates a list of referenceable parameters using DSL syntax.
 *
 * @param block configuration block for the parameters
 * @return list of Referenceable<Parameter> objects
 */
inline fun referenceableParameters(block: ReferenceableParametersBuilder.() -> Unit): List<Referenceable<Parameter>> {
    return ReferenceableParametersBuilder().apply(block).build()
}

// ============================================
// Security Requirement DSL
// ============================================

/**
 * DSL builder for security requirements list.
 *
 * Example usage:
 * ```kotlin
 * val security = securityRequirements {
 *     requirement("api_key")
 *     requirement("oauth2", "read:users", "write:users")
 * }
 * ```
 */
class SecurityRequirementsBuilder {
    @PublishedApi
    internal val requirements = mutableListOf<SecurityRequirement>()

    /**
     * Adds a security requirement with optional scopes.
     */
    fun requirement(schemeName: String, vararg scopes: String) {
        requirements.add(mapOf(schemeName to scopes.toList()))
    }

    /**
     * Adds multiple security requirements (AND logic within the map).
     */
    fun requirements(vararg schemes: Pair<String, List<String>>) {
        requirements.add(schemes.toMap())
    }

    fun build(): List<SecurityRequirement> = requirements.toList()
}

/**
 * Creates a list of security requirements using DSL syntax.
 *
 * @param block configuration block for the security requirements
 * @return list of SecurityRequirement objects
 */
inline fun securityRequirements(block: SecurityRequirementsBuilder.() -> Unit): List<SecurityRequirement> {
    return SecurityRequirementsBuilder().apply(block).build()
}

// ============================================
// Operation DSL
// ============================================

/**
 * DSL builder for [Operation] object.
 *
 * The builder enforces fail-fast validation:
 * - `responses` is required and must contain at least one response
 *
 * Example usage:
 * ```kotlin
 * val operation = operation {
 *     summary = "Get user by ID"
 *     description = "Returns a single user"
 *     operationId = "getUserById"
 *     tags("users")
 *     parameters {
 *         pathParameter("userId", "The user ID") {
 *             format = "uuid"
 *         }
 *     }
 *     responses {
 *         ok {
 *             description = "Successful response"
 *             content {
 *                 "application/json" to {
 *                     schema { type = "object" }
 *                 }
 *             }
 *         }
 *         notFound {
 *             description = "User not found"
 *         }
 *     }
 * }
 * ```
 */
class OperationBuilder {
    var summary: String? = null
    var description: String? = null
    var operationId: String? = null
    var externalDocs: ExternalDocumentation? = null
    var tags: List<String>? = null
    var parameters: List<Referenceable<Parameter>>? = null
    var requestBody: RequestBody? = null
    var responses: Responses? = null
    var callbacks: Map<String, Callback>? = null
    var deprecated: Boolean = false
    var security: List<SecurityRequirement>? = null
    var servers: List<Server>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Sets tags from varargs.
     */
    fun tags(vararg tagNames: String) {
        tags = tagNames.toList()
    }

    /**
     * Configures external documentation using DSL syntax.
     */
    inline fun externalDocs(block: ExternalDocumentationBuilder.() -> Unit) {
        externalDocs = externalDocumentation(block)
    }

    /**
     * Configures parameters using DSL syntax with support for references.
     */
    inline fun parameters(block: ReferenceableParametersBuilder.() -> Unit) {
        parameters = referenceableParameters(block)
    }

    /**
     * Configures the request body using DSL syntax.
     */
    inline fun requestBody(block: RequestBodyBuilder.() -> Unit) {
        requestBody = io.heapy.komok.tech.api.dsl.requestBody(block)
    }

    /**
     * Configures responses using DSL syntax.
     */
    inline fun responses(block: ResponsesBuilder.() -> Unit) {
        responses = io.heapy.komok.tech.api.dsl.responses(block)
    }

    /**
     * Configures callbacks using DSL syntax.
     */
    inline fun callbacks(block: CallbacksBuilder.() -> Unit) {
        callbacks = io.heapy.komok.tech.api.dsl.callbacks(block)
    }

    /**
     * Configures security requirements using DSL syntax.
     */
    inline fun security(block: SecurityRequirementsBuilder.() -> Unit) {
        security = securityRequirements(block)
    }

    /**
     * Configures servers using DSL syntax.
     */
    inline fun servers(block: ServersBuilder.() -> Unit) {
        servers = io.heapy.komok.tech.api.dsl.servers(block)
    }

    fun build(): Operation {
        val operationResponses = requireNotNull(responses) {
            "Operation responses is required"
        }
        // Additional validation is handled by Operation's init block
        return Operation(
            responses = operationResponses,
            tags = tags,
            summary = summary,
            description = description,
            externalDocs = externalDocs,
            operationId = operationId,
            parameters = parameters,
            requestBody = requestBody,
            callbacks = callbacks,
            deprecated = deprecated,
            security = security,
            servers = servers,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [Operation] object using DSL syntax.
 *
 * @param block configuration block for the operation
 * @return configured Operation object
 * @throws IllegalArgumentException if responses is not provided or is empty
 */
inline fun operation(block: OperationBuilder.() -> Unit): Operation {
    return OperationBuilder().apply(block).build()
}

// ============================================
// Callback DSL
// ============================================

/**
 * DSL builder for a single [Callback] object.
 *
 * A callback is a map of runtime expressions to PathItem objects.
 *
 * Example usage:
 * ```kotlin
 * val callback = callback {
 *     "{${'$'}request.body#/callbackUrl}" to {
 *         post {
 *             summary = "Webhook notification"
 *             responses {
 *                 ok { description = "Webhook received" }
 *             }
 *         }
 *     }
 * }
 * ```
 */
class CallbackBuilder {
    @PublishedApi
    internal val pathItems = mutableMapOf<String, PathItem>()

    /**
     * Adds a path item for a runtime expression using DSL syntax.
     */
    inline infix fun String.to(block: PathItemBuilder.() -> Unit) {
        pathItems[this] = pathItem(block)
    }

    /**
     * Adds a pre-built path item for a runtime expression.
     */
    infix fun String.to(pathItem: PathItem) {
        pathItems[this] = pathItem
    }

    fun build(): Callback = pathItems.toMap()
}

/**
 * Creates a [Callback] object using DSL syntax.
 *
 * @param block configuration block for the callback
 * @return configured Callback (Map<String, PathItem>)
 */
inline fun callback(block: CallbackBuilder.() -> Unit): Callback {
    return CallbackBuilder().apply(block).build()
}

/**
 * DSL builder for multiple callbacks map.
 *
 * Example usage:
 * ```kotlin
 * val callbacks = callbacks {
 *     "onEvent" to {
 *         "{${'$'}request.body#/callbackUrl}" to {
 *             post {
 *                 responses { ok { description = "OK" } }
 *             }
 *         }
 *     }
 * }
 * ```
 */
class CallbacksBuilder {
    @PublishedApi
    internal val callbacks = mutableMapOf<String, Callback>()

    /**
     * Adds a callback using DSL syntax.
     */
    inline infix fun String.to(block: CallbackBuilder.() -> Unit) {
        callbacks[this] = callback(block)
    }

    /**
     * Adds a pre-built callback.
     */
    infix fun String.to(callback: Callback) {
        callbacks[this] = callback
    }

    fun build(): Map<String, Callback> = callbacks.toMap()
}

/**
 * Creates a map of callbacks using DSL syntax.
 *
 * @param block configuration block for the callbacks
 * @return map of callback names to Callback objects
 */
inline fun callbacks(block: CallbacksBuilder.() -> Unit): Map<String, Callback> {
    return CallbacksBuilder().apply(block).build()
}

// ============================================
// PathItem DSL
// ============================================

/**
 * DSL builder for [PathItem] object.
 *
 * Example usage:
 * ```kotlin
 * val pathItem = pathItem {
 *     summary = "User operations"
 *     description = "Operations for managing users"
 *
 *     parameters {
 *         pathParameter("userId") { format = "uuid" }
 *     }
 *
 *     get {
 *         summary = "Get user"
 *         operationId = "getUser"
 *         responses {
 *             ok { description = "Success" }
 *         }
 *     }
 *
 *     put {
 *         summary = "Update user"
 *         operationId = "updateUser"
 *         requestBody {
 *             content {
 *                 "application/json" to { schema { type = "object" } }
 *             }
 *         }
 *         responses {
 *             ok { description = "Updated" }
 *         }
 *     }
 *
 *     delete {
 *         summary = "Delete user"
 *         operationId = "deleteUser"
 *         responses {
 *             noContent { description = "Deleted" }
 *         }
 *     }
 * }
 * ```
 */
class PathItemBuilder {
    var ref: String? = null
    var summary: String? = null
    var description: String? = null
    var servers: List<Server>? = null
    var parameters: List<Referenceable<Parameter>>? = null
    var get: Operation? = null
    var put: Operation? = null
    var post: Operation? = null
    var delete: Operation? = null
    var options: Operation? = null
    var head: Operation? = null
    var patch: Operation? = null
    var trace: Operation? = null
    var query: Operation? = null
    var additionalOperations: Map<String, Operation>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures servers using DSL syntax.
     */
    inline fun servers(block: ServersBuilder.() -> Unit) {
        servers = io.heapy.komok.tech.api.dsl.servers(block)
    }

    /**
     * Configures path-level parameters using DSL syntax.
     */
    inline fun parameters(block: ReferenceableParametersBuilder.() -> Unit) {
        parameters = referenceableParameters(block)
    }

    /**
     * Configures the GET operation using DSL syntax.
     */
    inline fun get(block: OperationBuilder.() -> Unit) {
        get = operation(block)
    }

    /**
     * Configures the PUT operation using DSL syntax.
     */
    inline fun put(block: OperationBuilder.() -> Unit) {
        put = operation(block)
    }

    /**
     * Configures the POST operation using DSL syntax.
     */
    inline fun post(block: OperationBuilder.() -> Unit) {
        post = operation(block)
    }

    /**
     * Configures the DELETE operation using DSL syntax.
     */
    inline fun delete(block: OperationBuilder.() -> Unit) {
        delete = operation(block)
    }

    /**
     * Configures the OPTIONS operation using DSL syntax.
     */
    inline fun options(block: OperationBuilder.() -> Unit) {
        options = operation(block)
    }

    /**
     * Configures the HEAD operation using DSL syntax.
     */
    inline fun head(block: OperationBuilder.() -> Unit) {
        head = operation(block)
    }

    /**
     * Configures the PATCH operation using DSL syntax.
     */
    inline fun patch(block: OperationBuilder.() -> Unit) {
        patch = operation(block)
    }

    /**
     * Configures the TRACE operation using DSL syntax.
     */
    inline fun trace(block: OperationBuilder.() -> Unit) {
        trace = operation(block)
    }

    /**
     * Configures the QUERY operation using DSL syntax (OpenAPI 3.2+).
     */
    inline fun query(block: OperationBuilder.() -> Unit) {
        query = operation(block)
    }

    /**
     * Configures additional (custom) HTTP method operations.
     */
    inline fun additionalOperations(block: AdditionalOperationsBuilder.() -> Unit) {
        additionalOperations = io.heapy.komok.tech.api.dsl.additionalOperations(block)
    }

    fun build(): PathItem {
        return PathItem(
            ref = ref,
            summary = summary,
            description = description,
            servers = servers,
            parameters = parameters,
            get = get,
            put = put,
            post = post,
            delete = delete,
            options = options,
            head = head,
            patch = patch,
            trace = trace,
            query = query,
            additionalOperations = additionalOperations,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [PathItem] object using DSL syntax.
 *
 * @param block configuration block for the path item
 * @return configured PathItem object
 */
inline fun pathItem(block: PathItemBuilder.() -> Unit): PathItem {
    return PathItemBuilder().apply(block).build()
}

/**
 * DSL builder for additional (custom) HTTP method operations.
 *
 * Example usage:
 * ```kotlin
 * additionalOperations {
 *     "CUSTOM" to {
 *         summary = "Custom operation"
 *         responses { ok { description = "OK" } }
 *     }
 * }
 * ```
 */
class AdditionalOperationsBuilder {
    @PublishedApi
    internal val operations = mutableMapOf<String, Operation>()

    /**
     * Adds an operation for a custom HTTP method using DSL syntax.
     */
    inline infix fun String.to(block: OperationBuilder.() -> Unit) {
        operations[this] = operation(block)
    }

    /**
     * Adds a pre-built operation for a custom HTTP method.
     */
    infix fun String.to(operation: Operation) {
        operations[this] = operation
    }

    fun build(): Map<String, Operation> = operations.toMap()
}

/**
 * Creates a map of additional operations using DSL syntax.
 *
 * @param block configuration block for the additional operations
 * @return map of HTTP method names to Operation objects
 */
inline fun additionalOperations(block: AdditionalOperationsBuilder.() -> Unit): Map<String, Operation> {
    return AdditionalOperationsBuilder().apply(block).build()
}

// ============================================
// Paths Container DSL
// ============================================

/**
 * DSL builder for [Paths] container.
 *
 * The builder enforces validation:
 * - All paths must start with a forward slash (/)
 *
 * Example usage:
 * ```kotlin
 * val paths = paths {
 *     "/users" to {
 *         get {
 *             summary = "List users"
 *             responses { ok { description = "User list" } }
 *         }
 *         post {
 *             summary = "Create user"
 *             requestBody {
 *                 content {
 *                     "application/json" to { schema { type = "object" } }
 *                 }
 *             }
 *             responses { created { description = "User created" } }
 *         }
 *     }
 *
 *     "/users/{userId}" to {
 *         parameters {
 *             pathParameter("userId") { format = "uuid" }
 *         }
 *         get {
 *             summary = "Get user by ID"
 *             responses { ok { description = "User details" } }
 *         }
 *         delete {
 *             summary = "Delete user"
 *             responses { noContent { description = "User deleted" } }
 *         }
 *     }
 * }
 * ```
 */
class PathsBuilder {
    @PublishedApi
    internal val paths = mutableMapOf<String, PathItem>()

    /**
     * Adds a path item using DSL syntax.
     *
     * @throws IllegalArgumentException if the path doesn't start with "/"
     */
    inline infix fun String.to(block: PathItemBuilder.() -> Unit) {
        validatePath(this)
        paths[this] = pathItem(block)
    }

    /**
     * Adds a pre-built path item.
     *
     * @throws IllegalArgumentException if the path doesn't start with "/"
     */
    infix fun String.to(pathItem: PathItem) {
        validatePath(this)
        paths[this] = pathItem
    }

    @PublishedApi
    internal fun validatePath(path: String) {
        require(path.startsWith("/")) {
            "Path '$path' must start with a forward slash (/)"
        }
    }

    fun build(): Paths = paths.toMap()
}

/**
 * Creates a [Paths] map using DSL syntax.
 *
 * @param block configuration block for the paths
 * @return map of path patterns to PathItem objects
 * @throws IllegalArgumentException if any path doesn't start with "/"
 */
inline fun paths(block: PathsBuilder.() -> Unit): Paths {
    return PathsBuilder().apply(block).build()
}
