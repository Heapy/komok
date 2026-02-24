package io.heapy.komok.tech.api.dsl

/**
 * Exception thrown when a `$ref` JSON Pointer cannot be resolved.
 *
 * @property ref the original reference string that failed to resolve
 */
class ReferenceResolutionException(
    message: String,
    val ref: String,
) : RuntimeException(message)

/**
 * Resolves `$ref` JSON Pointer strings to actual objects within an OpenAPI document.
 *
 * Only local references (`#/components/<type>/<name>`) are supported.
 *
 * @param document the OpenAPI document to resolve references against
 */
class ReferenceResolver(
    private val document: OpenAPI,
) {
    /**
     * Resolves a `$ref` JSON Pointer string to the referenced [OpenAPIObject].
     *
     * @param ref a JSON Pointer string, e.g. `#/components/schemas/Pet`
     * @return the resolved object
     * @throws ReferenceResolutionException if the reference cannot be resolved
     */
    fun resolve(ref: String): OpenAPIObject {
        return resolve(ref, depth = 0)
    }

    /**
     * Resolves a [Reference] object to the referenced [OpenAPIObject].
     *
     * @param reference the Reference to resolve
     * @return the resolved object
     * @throws ReferenceResolutionException if the reference cannot be resolved
     */
    fun resolve(reference: Reference): OpenAPIObject {
        return resolve(reference.ref)
    }

    /**
     * Unwraps a [Referenceable] value: returns the direct value or resolves the reference.
     *
     * @param T the expected concrete type
     * @param referenceable the value to unwrap
     * @return the resolved object
     * @throws ReferenceResolutionException if a reference cannot be resolved
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : OpenAPIObject> resolveReferenceable(referenceable: Referenceable<T>): T {
        return when (referenceable) {
            is Direct<T> -> referenceable.value
            is Reference -> resolve(referenceable.ref) as T
        }
    }

    private fun resolve(ref: String, depth: Int): OpenAPIObject {
        if (depth > MAX_CHAIN_DEPTH) {
            throw ReferenceResolutionException(
                "Reference chain depth exceeded $MAX_CHAIN_DEPTH (possible circular reference): $ref",
                ref,
            )
        }

        if (!ref.startsWith("#/")) {
            throw ReferenceResolutionException(
                "Only local references starting with '#/' are supported: $ref",
                ref,
            )
        }

        val pointer = ref.removePrefix("#/")
        if (pointer.isEmpty()) {
            throw ReferenceResolutionException(
                "Empty JSON Pointer: $ref",
                ref,
            )
        }

        val segments = pointer.split("/").map { unescapeJsonPointer(it) }

        if (segments.size != 3) {
            throw ReferenceResolutionException(
                "Expected exactly 3 path segments (components/<type>/<name>), got ${segments.size}: $ref",
                ref,
            )
        }

        val (root, type, name) = segments

        if (root != "components") {
            throw ReferenceResolutionException(
                "Only '#/components/...' references are supported, got root '$root': $ref",
                ref,
            )
        }

        val components = document.components
            ?: throw ReferenceResolutionException(
                "Document has no components section: $ref",
                ref,
            )

        return resolveComponent(components, type, name, ref, depth)
    }

    private fun resolveComponent(
        components: Components,
        type: String,
        name: String,
        ref: String,
        depth: Int,
    ): OpenAPIObject {
        return when (type) {
            "schemas" -> lookupOrThrow(components.schemas, name, ref)
            "responses" -> lookupOrThrow(components.responses, name, ref)
            "parameters" -> lookupOrThrow(components.parameters, name, ref)
            "examples" -> lookupOrThrow(components.examples, name, ref)
            "requestBodies" -> lookupOrThrow(components.requestBodies, name, ref)
            "headers" -> lookupOrThrow(components.headers, name, ref)
            "securitySchemes" -> lookupOrThrow(components.securitySchemes, name, ref)
            "links" -> lookupOrThrow(components.links, name, ref)
            "callbacks" -> resolveReferenceable(lookupOrThrow(components.callbacks, name, ref), ref, depth)
            "pathItems" -> lookupOrThrow(components.pathItems, name, ref)
            "mediaTypes" -> resolveReferenceable(lookupOrThrow(components.mediaTypes, name, ref), ref, depth)
            else -> throw ReferenceResolutionException(
                "Unknown component type '$type': $ref",
                ref,
            )
        }
    }

    private fun <T : OpenAPIObject> lookupOrThrow(
        map: Map<String, T>?,
        name: String,
        ref: String,
    ): T {
        if (map == null) {
            throw ReferenceResolutionException(
                "Component map is null for reference: $ref",
                ref,
            )
        }
        return map[name]
            ?: throw ReferenceResolutionException(
                "Component '$name' not found: $ref",
                ref,
            )
    }

    private fun <T : OpenAPIObject> resolveReferenceable(
        referenceable: Referenceable<T>,
        ref: String,
        depth: Int,
    ): OpenAPIObject {
        return when (referenceable) {
            is Direct<T> -> referenceable.value
            is Reference -> resolve(referenceable.ref, depth + 1)
        }
    }

    companion object {
        private const val MAX_CHAIN_DEPTH = 10

        /**
         * Unescapes a single JSON Pointer segment per RFC 6901:
         * `~1` → `/`, `~0` → `~`
         */
        private fun unescapeJsonPointer(segment: String): String {
            return segment
                .replace("~1", "/")
                .replace("~0", "~")
        }
    }
}
