package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

/**
 * DSL builder for [MediaType] object.
 *
 * The builder enforces fail-fast validation:
 * - `encoding` and (`prefixEncoding` or `itemEncoding`) are mutually exclusive
 * - `example` and `examples` are mutually exclusive
 *
 * Example usage:
 * ```kotlin
 * val mediaType = mediaType {
 *     schema {
 *         type = "object"
 *         properties {
 *             "name" to stringSchema()
 *         }
 *     }
 *     example = buildJsonObject {
 *         put("name", "John")
 *     }
 * }
 * ```
 */
class MediaTypeBuilder {
    var description: String? = null
    var schema: Schema? = null
    var itemSchema: Schema? = null
    var encoding: Map<String, Encoding>? = null
    var prefixEncoding: List<Encoding>? = null
    var itemEncoding: Encoding? = null
    var example: JsonElement? = null
    var examples: Map<String, Example>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the schema using DSL syntax.
     */
    inline fun schema(block: SchemaBuilder.() -> Unit) {
        schema = io.heapy.komok.tech.api.dsl.schema(block)
    }

    /**
     * Configures the item schema using DSL syntax.
     */
    inline fun itemSchema(block: SchemaBuilder.() -> Unit) {
        itemSchema = io.heapy.komok.tech.api.dsl.schema(block)
    }

    /**
     * Configures examples using DSL syntax.
     */
    inline fun examples(block: ExamplesBuilder.() -> Unit) {
        examples = io.heapy.komok.tech.api.dsl.examples(block)
    }

    fun build(): MediaType {
        // Validation is handled by MediaType's init block
        return MediaType(
            description = description,
            schema = schema,
            itemSchema = itemSchema,
            encoding = encoding,
            prefixEncoding = prefixEncoding,
            itemEncoding = itemEncoding,
            example = example,
            examples = examples,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [MediaType] object using DSL syntax.
 *
 * @param block configuration block for the media type
 * @return configured MediaType object
 */
inline fun mediaType(block: MediaTypeBuilder.() -> Unit): MediaType {
    return MediaTypeBuilder().apply(block).build()
}

/**
 * DSL builder for [Content] (Map<String, MediaType>).
 *
 * Example usage:
 * ```kotlin
 * val content = content {
 *     "application/json" to {
 *         schema {
 *             type = "object"
 *         }
 *     }
 *     "text/plain" to mediaType {
 *         schema {
 *             type = "string"
 *         }
 *     }
 * }
 * ```
 */
class ContentBuilder {
    @PublishedApi
    internal val content = mutableMapOf<String, MediaType>()

    /**
     * Adds a media type using DSL syntax.
     */
    inline infix fun String.to(block: MediaTypeBuilder.() -> Unit) {
        content[this] = mediaType(block)
    }

    /**
     * Adds a pre-built media type.
     */
    infix fun String.to(mediaType: MediaType) {
        content[this] = mediaType
    }

    fun build(): Content = content.toMap()
}

/**
 * Creates a [Content] map using DSL syntax.
 *
 * @param block configuration block for the content
 * @return map of media type names to MediaType objects
 */
inline fun content(block: ContentBuilder.() -> Unit): Content {
    return ContentBuilder().apply(block).build()
}

/**
 * DSL builder for [Parameter] object.
 *
 * The builder enforces fail-fast validation:
 * - `name` is required
 * - `location` is required
 * - Exactly one of `schema` or `content` must be present
 * - `querystring` location requires `content` (not `schema`)
 * - `path` location requires `required` to be true
 * - `content` must have exactly one entry
 * - `example` and `examples` are mutually exclusive
 * - `style`, `explode`, and `allowReserved` can only be used with `schema`
 * - `allowEmptyValue` can only be used with `query` location
 *
 * Example usage:
 * ```kotlin
 * val param = parameter {
 *     name = "userId"
 *     location = ParameterLocation.PATH
 *     required = true
 *     description = "The user ID"
 *     schema {
 *         type = "string"
 *         format = "uuid"
 *     }
 * }
 * ```
 */
class ParameterBuilder {
    var name: String? = null
    var location: ParameterLocation? = null
    var description: String? = null
    var required: Boolean = false
    var deprecated: Boolean = false
    var schema: Schema? = null
    var content: Content? = null
    var style: ParameterStyle? = null
    var explode: Boolean? = null
    var allowReserved: Boolean? = null
    var allowEmptyValue: Boolean? = null
    var example: JsonElement? = null
    var examples: Map<String, Example>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the schema using DSL syntax.
     */
    inline fun schema(block: SchemaBuilder.() -> Unit) {
        schema = io.heapy.komok.tech.api.dsl.schema(block)
    }

    /**
     * Configures the content using DSL syntax.
     */
    inline fun content(block: ContentBuilder.() -> Unit) {
        content = io.heapy.komok.tech.api.dsl.content(block)
    }

    /**
     * Configures examples using DSL syntax.
     */
    inline fun examples(block: ExamplesBuilder.() -> Unit) {
        examples = io.heapy.komok.tech.api.dsl.examples(block)
    }

    fun build(): Parameter {
        val paramName = requireNotNull(name) {
            "Parameter name is required"
        }
        val paramLocation = requireNotNull(location) {
            "Parameter location is required"
        }

        // Validation is handled by Parameter's init block
        return Parameter(
            name = paramName,
            location = paramLocation,
            description = description,
            required = required,
            deprecated = deprecated,
            schema = schema,
            content = content,
            style = style,
            explode = explode,
            allowReserved = allowReserved,
            allowEmptyValue = allowEmptyValue,
            example = example,
            examples = examples,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [Parameter] object using DSL syntax.
 *
 * @param block configuration block for the parameter
 * @return configured Parameter object
 * @throws IllegalArgumentException if validation fails
 */
inline fun parameter(block: ParameterBuilder.() -> Unit): Parameter {
    return ParameterBuilder().apply(block).build()
}

/**
 * DSL builder for creating a list of [Parameter] objects.
 *
 * Example usage:
 * ```kotlin
 * val params = parameters {
 *     parameter {
 *         name = "userId"
 *         location = ParameterLocation.PATH
 *         required = true
 *         schema { type = "string" }
 *     }
 *     parameter {
 *         name = "limit"
 *         location = ParameterLocation.QUERY
 *         schema { type = "integer" }
 *     }
 * }
 * ```
 */
class ParametersBuilder {
    @PublishedApi
    internal val parameters = mutableListOf<Parameter>()

    /**
     * Adds a parameter using DSL syntax.
     */
    inline fun parameter(block: ParameterBuilder.() -> Unit) {
        parameters.add(io.heapy.komok.tech.api.dsl.parameter(block))
    }

    /**
     * Adds a pre-built parameter.
     */
    fun parameter(parameter: Parameter) {
        parameters.add(parameter)
    }

    fun build(): List<Parameter> = parameters.toList()
}

/**
 * Creates a list of [Parameter] objects using DSL syntax.
 *
 * @param block configuration block for the parameters
 * @return list of configured Parameter objects
 */
inline fun parameters(block: ParametersBuilder.() -> Unit): List<Parameter> {
    return ParametersBuilder().apply(block).build()
}

/**
 * DSL builder for [Header] object.
 *
 * The builder enforces fail-fast validation:
 * - Exactly one of `schema` or `content` must be present
 * - `example` and `examples` are mutually exclusive
 *
 * Example usage:
 * ```kotlin
 * val header = header {
 *     description = "Rate limit remaining"
 *     required = true
 *     schema {
 *         type = "integer"
 *     }
 * }
 * ```
 */
class HeaderBuilder {
    var description: String? = null
    var required: Boolean = false
    var deprecated: Boolean = false
    var schema: Schema? = null
    var content: Map<String, MediaType>? = null
    var style: String? = null
    var explode: Boolean? = null
    var example: JsonElement? = null
    var examples: Map<String, Example>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the schema using DSL syntax.
     */
    inline fun schema(block: SchemaBuilder.() -> Unit) {
        schema = io.heapy.komok.tech.api.dsl.schema(block)
    }

    /**
     * Configures the content using DSL syntax.
     */
    inline fun content(block: ContentBuilder.() -> Unit) {
        content = io.heapy.komok.tech.api.dsl.content(block)
    }

    /**
     * Configures examples using DSL syntax.
     */
    inline fun examples(block: ExamplesBuilder.() -> Unit) {
        examples = io.heapy.komok.tech.api.dsl.examples(block)
    }

    fun build(): Header {
        // Validation is handled by Header's init block
        return Header(
            description = description,
            required = required,
            deprecated = deprecated,
            schema = schema,
            content = content,
            style = style,
            explode = explode,
            example = example,
            examples = examples,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [Header] object using DSL syntax.
 *
 * @param block configuration block for the header
 * @return configured Header object
 * @throws IllegalArgumentException if validation fails
 */
inline fun header(block: HeaderBuilder.() -> Unit): Header {
    return HeaderBuilder().apply(block).build()
}

/**
 * DSL builder for creating a map of named [Header] objects.
 *
 * Example usage:
 * ```kotlin
 * val headers = headers {
 *     "X-Rate-Limit-Remaining" to {
 *         description = "Number of requests remaining"
 *         schema { type = "integer" }
 *     }
 *     "X-Request-Id" to {
 *         description = "Unique request identifier"
 *         schema { type = "string" }
 *     }
 * }
 * ```
 */
class HeadersBuilder {
    @PublishedApi
    internal val headers = mutableMapOf<String, Header>()

    /**
     * Adds a header using DSL syntax.
     */
    inline infix fun String.to(block: HeaderBuilder.() -> Unit) {
        headers[this] = header(block)
    }

    /**
     * Adds a pre-built header.
     */
    infix fun String.to(header: Header) {
        headers[this] = header
    }

    fun build(): Map<String, Header> = headers.toMap()
}

/**
 * Creates a map of named [Header] objects using DSL syntax.
 *
 * @param block configuration block for the headers
 * @return map of header names to Header objects
 */
inline fun headers(block: HeadersBuilder.() -> Unit): Map<String, Header> {
    return HeadersBuilder().apply(block).build()
}

// ============================================
// Convenience functions for common Parameter patterns
// ============================================

/**
 * Creates a path parameter with string schema.
 *
 * Example usage:
 * ```kotlin
 * val param = pathParameter("userId") {
 *     format = "uuid"
 * }
 * ```
 */
inline fun pathParameter(
    name: String,
    description: String? = null,
    block: SchemaBuilder.() -> Unit = {},
): Parameter {
    return parameter {
        this.name = name
        location = ParameterLocation.PATH
        required = true
        this.description = description
        schema(block)
    }
}

/**
 * Creates a query parameter with string schema.
 *
 * Example usage:
 * ```kotlin
 * val param = queryParameter("filter") {
 *     description = "Filter criteria"
 *     schema { type = "string" }
 * }
 * ```
 */
inline fun queryParameter(
    name: String,
    description: String? = null,
    required: Boolean = false,
    block: SchemaBuilder.() -> Unit = {},
): Parameter {
    return parameter {
        this.name = name
        location = ParameterLocation.QUERY
        this.required = required
        this.description = description
        schema(block)
    }
}

/**
 * Creates a header parameter with string schema.
 *
 * Example usage:
 * ```kotlin
 * val param = headerParameter("X-Api-Key") {
 *     description = "API Key"
 *     schema { type = "string" }
 * }
 * ```
 */
inline fun headerParameter(
    name: String,
    description: String? = null,
    required: Boolean = false,
    block: SchemaBuilder.() -> Unit = {},
): Parameter {
    return parameter {
        this.name = name
        location = ParameterLocation.HEADER
        this.required = required
        this.description = description
        schema(block)
    }
}

/**
 * Creates a cookie parameter with string schema.
 *
 * Example usage:
 * ```kotlin
 * val param = cookieParameter("session") {
 *     description = "Session ID"
 *     schema { type = "string" }
 * }
 * ```
 */
inline fun cookieParameter(
    name: String,
    description: String? = null,
    required: Boolean = false,
    block: SchemaBuilder.() -> Unit = {},
): Parameter {
    return parameter {
        this.name = name
        location = ParameterLocation.COOKIE
        this.required = required
        this.description = description
        schema(block)
    }
}
