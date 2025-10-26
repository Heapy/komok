package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Describes the operations available on a single path.
 *
 * A Path Item may be empty due to ACL constraints. The path itself is still exposed
 * to the documentation viewer but they will not know which operations and parameters are available.
 *
 * @property ref Allows for a referenced definition of this path item (using $ref)
 * @property summary A summary that applies to all operations in this path item
 * @property description A description that applies to all operations in this path item
 * @property servers An alternative server array to service all operations in this path
 * @property parameters A list of parameters that are applicable for all the operations described under this path
 * @property get Definition of a GET operation on this path
 * @property put Definition of a PUT operation on this path
 * @property post Definition of a POST operation on this path
 * @property delete Definition of a DELETE operation on this path
 * @property options Definition of an OPTIONS operation on this path
 * @property head Definition of a HEAD operation on this path
 * @property patch Definition of a PATCH operation on this path
 * @property trace Definition of a TRACE operation on this path
 * @property query Definition of a QUERY operation on this path (OpenAPI 3.2+)
 * @property additionalOperations Map of additional custom HTTP methods (keys must not be standard method names in uppercase)
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#path-item-object">Path Item Object</a>
 */
@Serializable
data class PathItem(
    @SerialName("\$ref")
    val ref: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val servers: List<Server>? = null,
    val parameters: List<Parameter>? = null,
    val get: Operation? = null,
    val put: Operation? = null,
    val post: Operation? = null,
    val delete: Operation? = null,
    val options: Operation? = null,
    val head: Operation? = null,
    val patch: Operation? = null,
    val trace: Operation? = null,
    val query: Operation? = null,
    val additionalOperations: Map<String, Operation>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate that additionalOperations keys are not standard method names in uppercase
        additionalOperations?.keys?.forEach { method ->
            require(method !in STANDARD_HTTP_METHODS) {
                "Additional operation method '$method' cannot be a standard HTTP method name in uppercase. " +
                        "Standard methods (GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE, QUERY) " +
                        "should use the dedicated properties instead."
            }
        }

        // Validate that additionalOperations keys match the RFC9110 pattern for method names
        additionalOperations?.keys?.forEach { method ->
            require(method.matches(Regex("^[a-zA-Z0-9!#\$%&'*+.^_`|~-]+$"))) {
                "Additional operation method '$method' must match RFC9110 pattern for HTTP methods (1*tchar)"
            }
        }
    }

    companion object {
        private val STANDARD_HTTP_METHODS = setOf(
            "GET", "PUT", "POST", "DELETE", "OPTIONS", "HEAD", "PATCH", "TRACE", "QUERY"
        )
    }
}

/**
 * Holds the relative paths to the individual endpoints and their operations.
 *
 * The path is appended to the URL from the Server Object in order to construct the full URL.
 * The Paths may be empty, due to Access Control List (ACL) constraints.
 *
 * Path patterns:
 * - Must start with a forward slash (/)
 * - May contain path parameters in curly braces (e.g., /users/{id})
 * - Path templating is allowed
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#paths-object">Paths Object</a>
 */
typealias Paths = Map<String, PathItem>

/**
 * Creates a Paths map from vararg pairs with validation.
 *
 * @param pairs key-value pairs where keys are path patterns (must start with "/")
 * @return map of paths
 * @throws IllegalArgumentException if any path doesn't start with "/"
 */
fun paths(vararg pairs: Pair<String, PathItem>): Paths {
    pairs.forEach { (path, _) ->
        require(path.startsWith("/")) {
            "Path '$path' must start with a forward slash (/)"
        }
    }

    return mapOf(*pairs)
}
