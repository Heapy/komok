package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

/**
 * Type alias for specification extensions.
 *
 * Specification extensions are arbitrary properties with names starting with "x-"
 * that allow custom metadata in OpenAPI documents.
 *
 * The values can be any valid JSON value.
 */
typealias SpecificationExtensions = Map<String, JsonElement>

/**
 * Creates a map of specification extensions from pairs.
 *
 * @param pairs key-value pairs where keys should start with "x-"
 * @return map of specification extensions
 * @throws IllegalArgumentException if any key doesn't start with "x-"
 */
fun specificationExtensions(vararg pairs: Pair<String, JsonElement>): SpecificationExtensions {
    pairs.forEach { (key, _) ->
        require(key.startsWith("x-")) {
            "Specification extension keys must start with 'x-', but got: '$key'"
        }
    }
    return mapOf(*pairs)
}

/**
 * Creates an empty map of specification extensions.
 */
fun emptySpecificationExtensions(): SpecificationExtensions = emptyMap()
