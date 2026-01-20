package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

// ============================================
// RequestBody DSL Builders
// ============================================

/**
 * DSL builder for [RequestBody] object.
 *
 * The builder enforces fail-fast validation:
 * - `content` is required and must not be empty
 *
 * Example usage:
 * ```kotlin
 * val requestBody = requestBody {
 *     description = "User object to create"
 *     required = true
 *     content {
 *         "application/json" to {
 *             schema {
 *                 type = "object"
 *                 properties {
 *                     "name" to stringSchema()
 *                     "email" to stringSchema { format = "email" }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
class RequestBodyBuilder {
    var description: String? = null
    var content: Content? = null
    var required: Boolean = false
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the content using DSL syntax.
     *
     * Example:
     * ```kotlin
     * requestBody {
     *     content {
     *         "application/json" to {
     *             schema { type = "object" }
     *         }
     *     }
     * }
     * ```
     */
    inline fun content(block: ContentBuilder.() -> Unit) {
        content = io.heapy.komok.tech.api.dsl.content(block)
    }

    fun build(): RequestBody {
        val requestContent = requireNotNull(content) {
            "RequestBody content is required"
        }
        // Additional validation is handled by RequestBody's init block
        return RequestBody(
            content = requestContent,
            description = description,
            required = required,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [RequestBody] object using DSL syntax.
 *
 * @param block configuration block for the request body
 * @return configured RequestBody object
 * @throws IllegalArgumentException if content is not provided or is empty
 */
inline fun requestBody(block: RequestBodyBuilder.() -> Unit): RequestBody {
    return RequestBodyBuilder().apply(block).build()
}

// ============================================
// Response DSL Builders
// ============================================

/**
 * DSL builder for referenceable headers map.
 *
 * Allows building a map of headers that can be either inline definitions
 * or references to components.
 *
 * Example usage:
 * ```kotlin
 * val headers = referenceableHeaders {
 *     "X-Rate-Limit" to {
 *         description = "Requests remaining"
 *         schema { type = "integer" }
 *     }
 *     "X-Request-Id" toRef "#/components/headers/RequestId"
 * }
 * ```
 */
class ReferenceableHeadersBuilder {
    @PublishedApi
    internal val headers = mutableMapOf<String, Referenceable<Header>>()

    /**
     * Adds an inline header using DSL syntax.
     */
    inline infix fun String.to(block: HeaderBuilder.() -> Unit) {
        headers[this] = Direct(header(block))
    }

    /**
     * Adds a pre-built header as inline.
     */
    infix fun String.to(header: Header) {
        headers[this] = Direct(header)
    }

    /**
     * Adds a reference to a header component.
     */
    infix fun String.toRef(ref: String) {
        headers[this] = Reference(ref = ref)
    }

    /**
     * Adds a reference with summary and description.
     */
    fun ref(name: String, ref: String, summary: String? = null, description: String? = null) {
        headers[name] = Reference(ref = ref, summary = summary, description = description)
    }

    fun build(): Map<String, Referenceable<Header>> = headers.toMap()
}

/**
 * Creates a map of referenceable headers using DSL syntax.
 *
 * @param block configuration block for the headers
 * @return map of header names to Referenceable<Header> objects
 */
inline fun referenceableHeaders(block: ReferenceableHeadersBuilder.() -> Unit): Map<String, Referenceable<Header>> {
    return ReferenceableHeadersBuilder().apply(block).build()
}

/**
 * DSL builder for [Response] object.
 *
 * Example usage:
 * ```kotlin
 * val response = response {
 *     description = "Successful response"
 *     summary = "Success"
 *     headers {
 *         "X-Rate-Limit" to {
 *             description = "Rate limit remaining"
 *             schema { type = "integer" }
 *         }
 *     }
 *     content {
 *         "application/json" to {
 *             schema {
 *                 type = "object"
 *             }
 *         }
 *     }
 * }
 * ```
 */
class ResponseBuilder {
    var description: String? = null
    var summary: String? = null
    var headers: Map<String, Referenceable<Header>>? = null
    var content: Content? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures headers using DSL syntax with support for references.
     *
     * Example:
     * ```kotlin
     * response {
     *     headers {
     *         "X-Rate-Limit" to {
     *             schema { type = "integer" }
     *         }
     *         "X-Request-Id" toRef "#/components/headers/RequestId"
     *     }
     * }
     * ```
     */
    inline fun headers(block: ReferenceableHeadersBuilder.() -> Unit) {
        headers = referenceableHeaders(block)
    }

    /**
     * Configures the content using DSL syntax.
     *
     * Example:
     * ```kotlin
     * response {
     *     content {
     *         "application/json" to {
     *             schema { type = "object" }
     *         }
     *     }
     * }
     * ```
     */
    inline fun content(block: ContentBuilder.() -> Unit) {
        content = io.heapy.komok.tech.api.dsl.content(block)
    }

    fun build(): Response {
        return Response(
            description = description,
            summary = summary,
            headers = headers,
            content = content,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [Response] object using DSL syntax.
 *
 * @param block configuration block for the response
 * @return configured Response object
 */
inline fun response(block: ResponseBuilder.() -> Unit): Response {
    return ResponseBuilder().apply(block).build()
}

// ============================================
// Responses Container DSL
// ============================================

/**
 * DSL builder for [Responses] container.
 *
 * The builder enforces fail-fast validation:
 * - At least one response must be defined
 * - Status codes must be valid (100-599, 1XX-5XX patterns, or "default")
 *
 * Example usage:
 * ```kotlin
 * val responses = responses {
 *     "200" to {
 *         description = "Successful response"
 *         content {
 *             "application/json" to {
 *                 schema { type = "object" }
 *             }
 *         }
 *     }
 *     "400" to {
 *         description = "Bad request"
 *     }
 *     "5XX" to {
 *         description = "Server error"
 *     }
 *     default {
 *         description = "Unexpected error"
 *     }
 * }
 * ```
 */
class ResponsesBuilder {
    @PublishedApi
    internal val responses = mutableMapOf<String, Response>()

    /**
     * Adds a response for a specific status code using DSL syntax.
     *
     * @throws IllegalArgumentException if the status code is invalid
     */
    inline infix fun String.to(block: ResponseBuilder.() -> Unit) {
        validateStatusCode(this)
        responses[this] = response(block)
    }

    /**
     * Adds a pre-built response for a specific status code.
     *
     * @throws IllegalArgumentException if the status code is invalid
     */
    infix fun String.to(response: Response) {
        validateStatusCode(this)
        responses[this] = response
    }

    /**
     * Adds a default response using DSL syntax.
     * The default response is used when no other response matches.
     */
    inline fun default(block: ResponseBuilder.() -> Unit) {
        responses["default"] = response(block)
    }

    /**
     * Adds a pre-built default response.
     */
    fun default(response: Response) {
        responses["default"] = response
    }

    // Convenience methods for common status codes

    /**
     * Adds a 200 OK response.
     */
    inline fun ok(block: ResponseBuilder.() -> Unit) {
        responses["200"] = response(block)
    }

    /**
     * Adds a 201 Created response.
     */
    inline fun created(block: ResponseBuilder.() -> Unit) {
        responses["201"] = response(block)
    }

    /**
     * Adds a 204 No Content response.
     */
    inline fun noContent(block: ResponseBuilder.() -> Unit) {
        responses["204"] = response(block)
    }

    /**
     * Adds a 400 Bad Request response.
     */
    inline fun badRequest(block: ResponseBuilder.() -> Unit) {
        responses["400"] = response(block)
    }

    /**
     * Adds a 401 Unauthorized response.
     */
    inline fun unauthorized(block: ResponseBuilder.() -> Unit) {
        responses["401"] = response(block)
    }

    /**
     * Adds a 403 Forbidden response.
     */
    inline fun forbidden(block: ResponseBuilder.() -> Unit) {
        responses["403"] = response(block)
    }

    /**
     * Adds a 404 Not Found response.
     */
    inline fun notFound(block: ResponseBuilder.() -> Unit) {
        responses["404"] = response(block)
    }

    /**
     * Adds a 500 Internal Server Error response.
     */
    inline fun internalServerError(block: ResponseBuilder.() -> Unit) {
        responses["500"] = response(block)
    }

    @PublishedApi
    internal fun validateStatusCode(statusCode: String) {
        val isValid = when {
            statusCode == "default" -> true
            statusCode.matches(Regex("^[1-5]XX$")) -> true
            statusCode.matches(Regex("^[1-5][0-9]{2}$")) -> {
                val code = statusCode.toIntOrNull()
                code != null && code in 100..599
            }
            else -> false
        }

        require(isValid) {
            "Invalid status code: '$statusCode'. Must be a specific code (100-599), wildcard pattern (1XX-5XX), or 'default'"
        }
    }

    fun build(): Responses {
        require(responses.isNotEmpty()) {
            "Responses must contain at least one response"
        }
        return responses.toMap()
    }
}

/**
 * Creates a [Responses] map using DSL syntax.
 *
 * @param block configuration block for the responses
 * @return map of status codes to Response objects
 * @throws IllegalArgumentException if no responses are provided or if status codes are invalid
 */
inline fun responses(block: ResponsesBuilder.() -> Unit): Responses {
    return ResponsesBuilder().apply(block).build()
}
