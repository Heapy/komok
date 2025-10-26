package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Describes a single operation parameter.
 *
 * A parameter is uniquely identified by a combination of a name and location.
 *
 * Note: schema and content are mutually exclusive - exactly one must be present.
 * Note: example and examples are mutually exclusive.
 * Note: querystring location requires content (not schema).
 * Note: path location requires required to be true.
 *
 * @property name The name of the parameter (required). Parameter names are case sensitive
 * @property `in` The location of the parameter (required)
 * @property description A brief description of the parameter
 * @property required Determines whether this parameter is mandatory
 * @property deprecated Specifies that a parameter is deprecated
 * @property schema The schema defining the type used for the parameter (mutually exclusive with content)
 * @property content A map containing the representations for the parameter (mutually exclusive with schema)
 * @property style Describes how the parameter value will be serialized
 * @property explode When true, generates separate parameters for array or object values
 * @property allowReserved Determines whether the parameter value should allow reserved characters
 * @property allowEmptyValue Sets the ability to pass empty-valued parameters (only for query parameters)
 * @property example Example of the parameter's potential value
 * @property examples Examples of the parameter's potential value
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#parameter-object">Parameter Object</a>
 */
@Serializable
data class Parameter(
    val name: String,
    @SerialName("in")
    val location: ParameterLocation,
    val description: String? = null,
    val required: Boolean = false,
    val deprecated: Boolean = false,
    val schema: Schema? = null,
    val content: Content? = null,
    val style: ParameterStyle? = null,
    val explode: Boolean? = null,
    val allowReserved: Boolean? = null,
    val allowEmptyValue: Boolean? = null,
    val example: JsonElement? = null,
    val examples: Map<String, Example>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate that exactly one of schema or content is present
        val hasSchema = schema != null
        val hasContent = content != null && content.isNotEmpty()

        require(hasSchema xor hasContent) {
            "Parameter must have exactly one of 'schema' or 'content' specified"
        }

        // Validate that querystring location requires content (not schema)
        if (location == ParameterLocation.QUERYSTRING) {
            require(hasContent) {
                "Parameter with location 'querystring' must use 'content' (not 'schema')"
            }
        }

        // Validate that path parameters must be required
        if (location == ParameterLocation.PATH) {
            require(required) {
                "Parameter with location 'path' must have 'required' set to true"
            }
        }

        // Validate that content has exactly one entry
        if (hasContent) {
            require(content.size == 1) {
                "Parameter 'content' must have exactly one media type entry"
            }
        }

        // Validate that example and examples are mutually exclusive
        require(!(example != null && examples != null)) {
            "Parameter 'example' and 'examples' are mutually exclusive"
        }

        // Validate that style-related properties are only used with schema
        if (!hasSchema) {
            require(style == null && explode == null && allowReserved == null) {
                "Parameter properties 'style', 'explode', and 'allowReserved' can only be used with 'schema'"
            }
        }

        // Validate that allowEmptyValue is only used with query parameters
        if (allowEmptyValue != null && location != ParameterLocation.QUERY) {
            error("Parameter property 'allowEmptyValue' can only be used with location 'query'")
        }
    }
}

/**
 * The location of the parameter.
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#parameter-object">Parameter Object</a>
 */
@Serializable
enum class ParameterLocation {
    /** Parameters that are appended to the URL in query string format */
    @SerialName("query")
    QUERY,

    /**
     * Parameters that use content instead of schema for complex query string serialization.
     * Note: querystring and query cannot both be present in the same parameters array.
     */
    @SerialName("querystring")
    QUERYSTRING,

    /** Custom headers that are expected as part of the request */
    @SerialName("header")
    HEADER,

    /**
     * Used together with Path Templating, where the parameter value is actually part of the operation's URL.
     * Path parameters are always required.
     */
    @SerialName("path")
    PATH,

    /** Used to pass a specific cookie value to the API */
    @SerialName("cookie")
    COOKIE,
}

/**
 * Describes how the parameter value will be serialized.
 *
 * Different styles are allowed for different parameter locations:
 * - Path: matrix, label, simple (default: simple)
 * - Query: form, spaceDelimited, pipeDelimited, deepObject (default: form)
 * - Header: simple (default: simple)
 * - Cookie: form, cookie (default: form)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#parameter-object">Parameter Object</a>
 * @see <a href="https://spec.openapis.org/oas/v3.2#style-values">Style Values</a>
 */
@Serializable
enum class ParameterStyle {
    /** Path-style parameters defined by RFC 6570 */
    @SerialName("matrix")
    MATRIX,

    /** Label-style parameters defined by RFC 6570 */
    @SerialName("label")
    LABEL,

    /** Simple-style parameters defined by RFC 6570 */
    @SerialName("simple")
    SIMPLE,

    /** Form-style parameters defined by RFC 6570 */
    @SerialName("form")
    FORM,

    /** Space separated array values */
    @SerialName("spaceDelimited")
    SPACE_DELIMITED,

    /** Pipe separated array values */
    @SerialName("pipeDelimited")
    PIPE_DELIMITED,

    /** Deep object serialization for nested objects */
    @SerialName("deepObject")
    DEEP_OBJECT,

    /** Cookie-style parameters */
    @SerialName("cookie")
    COOKIE,
}
