package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.Direct
import io.heapy.komok.tech.api.dsl.OpenAPI
import io.heapy.komok.tech.api.dsl.ParameterLocation
import io.heapy.komok.tech.api.dsl.Reference
import io.heapy.komok.tech.api.dsl.Schema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private val httpFileJson = Json { prettyPrint = true }

/**
 * Generates an HTTP file (.http) containing request templates for all operations
 * defined in the OpenAPI document.
 *
 * The generated file follows the HTTP file format supported by IntelliJ IDEA and VS Code REST Client.
 * Each operation produces a request block with:
 * - Summary and description as comments
 * - Parameter documentation
 * - Query parameters in the URL
 * - Content-Type and Accept headers
 * - Example request bodies (from explicit examples or generated from schemas)
 * - Expected response codes
 *
 * @param openapi The OpenAPI document to generate HTTP requests from
 * @return A string containing the HTTP file content
 */
fun generateHttpFile(openapi: OpenAPI): String = buildString {
    appendLine("# ${openapi.info.title} - HTTP Requests")
    appendLine("# Version: ${openapi.info.version}")
    openapi.info.description?.let { desc ->
        desc.lines().forEach { appendLine("# $it") }
    }
    appendLine()

    // Base URLs from servers
    val servers = openapi.servers
    if (servers != null && servers.size > 1) {
        servers.forEachIndexed { index, server ->
            val desc = server.description?.let { " # $it" } ?: ""
            if (index == 0) {
                appendLine("@baseUrl = ${server.url}$desc")
            } else {
                appendLine("# @baseUrl = ${server.url}$desc")
            }
        }
    } else {
        val baseUrl = servers?.firstOrNull()?.url ?: "{{baseUrl}}"
        appendLine("@baseUrl = $baseUrl")
    }
    appendLine()

    // Collect component schemas for $ref resolution
    val componentSchemas = openapi.components?.schemas ?: emptyMap()

    openapi.paths?.forEach { [path, pathItem] ->
        val operations = listOfNotNull(
            pathItem.get?.let { "GET" to it },
            pathItem.post?.let { "POST" to it },
            pathItem.put?.let { "PUT" to it },
            pathItem.delete?.let { "DELETE" to it },
            pathItem.patch?.let { "PATCH" to it },
            pathItem.options?.let { "OPTIONS" to it },
            pathItem.head?.let { "HEAD" to it },
            pathItem.trace?.let { "TRACE" to it },
        )

        operations.forEach { [method, operation] ->
            // Section header with summary
            val summary = operation.summary ?: "$method $path"
            appendLine("### $summary")

            // Description as comment
            operation.description?.let { desc ->
                desc.lines().forEach { appendLine("# $it") }
            }

            // Document parameters as comments
            val pathParams = mutableListOf<Pair<String, String?>>()
            val queryParams = mutableListOf<Triple<String, String?, Boolean>>()
            val headerParams = mutableListOf<Triple<String, String?, Boolean>>()

            operation.parameters?.forEach { paramRef ->
                if (paramRef is Direct) {
                    val param = paramRef.value
                    when (param.location) {
                        ParameterLocation.PATH -> {
                            pathParams.add(param.name to param.description)
                        }
                        ParameterLocation.QUERY, ParameterLocation.QUERYSTRING -> {
                            queryParams.add(Triple(param.name, param.description, param.required))
                        }
                        ParameterLocation.HEADER -> {
                            headerParams.add(Triple(param.name, param.description, param.required))
                        }
                        ParameterLocation.COOKIE -> {}
                    }
                }
            }

            // Document path parameters
            if (pathParams.isNotEmpty()) {
                pathParams.forEach { [name, desc] ->
                    val descSuffix = desc?.let { " - $it" } ?: ""
                    appendLine("# @param {$name}$descSuffix")
                }
            }

            // Document responses as comments (before request line to avoid being parsed as body)
            if (operation.responses.isNotEmpty()) {
                operation.responses.forEach { [statusCode, response] ->
                    val desc = response.description ?: response.summary ?: ""
                    appendLine("# $statusCode: $desc")
                }
            }

            // Build URL with path parameters replaced and query parameters appended
            var urlPath = path.replace(Regex("\\{([^}]+)}")) { "{{${it.groupValues[1]}}}" }
            if (queryParams.isNotEmpty()) {
                val queryString = queryParams.joinToString("&") { [name, _, _] ->
                    "$name={{$name}}"
                }
                urlPath = "$urlPath?$queryString"
            }

            appendLine("$method {{baseUrl}}$urlPath")

            // Unwrap request body reference
            val requestBody = when (val rb = operation.requestBody) {
                is Direct -> rb.value
                is Reference -> null
                null -> null
            }

            // Add headers
            var hasContentType = false
            requestBody?.let { rb ->
                val contentType = rb.content.keys.firstOrNull { it.contains("json") }
                    ?: rb.content.keys.firstOrNull()
                    ?: "application/json"
                appendLine("Content-Type: $contentType")
                hasContentType = true
            }

            // Add Accept header if responses have content
            val responseContentTypes = operation.responses.values
                .mapNotNull { it.content?.keys }
                .flatten()
                .distinct()
            if (responseContentTypes.isNotEmpty()) {
                val accept = responseContentTypes.firstOrNull { it.contains("json") }
                    ?: responseContentTypes.first()
                appendLine("Accept: $accept")
            }

            // Add custom header parameters
            headerParams.forEach { [name, desc, required] ->
                val reqMarker = if (required) "" else " (optional)"
                val descSuffix = desc?.let { " # $it$reqMarker" } ?: ""
                appendLine("$name: {{$name}}$descSuffix")
            }

            // Add request body
            requestBody?.let { rb ->
                val preferredKey = rb.content.keys.firstOrNull { it.contains("json") }
                    ?: rb.content.keys.firstOrNull()
                val mediaType = preferredKey?.let { key ->
                    when (val ref = rb.content[key]) {
                        is Direct -> ref.value
                        else -> null
                    }
                }

                // Try explicit example first
                val exampleBody = mediaType?.example
                    ?: mediaType?.examples?.values?.firstOrNull()?.value

                val body = if (exampleBody != null) {
                    httpFileJson.encodeToString(JsonElement.serializer(), exampleBody)
                } else {
                    // Generate example from schema
                    mediaType?.schema?.let { schema ->
                        val generated = generateExampleFromSchema(schema, componentSchemas)
                        if (generated != null) {
                            httpFileJson.encodeToString(JsonElement.serializer(), generated)
                        } else {
                            null
                        }
                    }
                }

                if (body != null) {
                    appendLine()
                    appendLine(body)
                }
            }

            appendLine()
            appendLine()
        }
    }
}

/**
 * Generates an example JSON value from an OpenAPI Schema, resolving `$ref` references
 * against the component schemas.
 *
 * Walks the schema structure to produce a representative example:
 * - Uses `example` values when present on properties
 * - Uses type defaults (0 for integer, "" for string, etc.) as fallbacks
 * - Resolves `$ref` to component schemas recursively (with depth limit)
 */
private fun generateExampleFromSchema(
    schema: Schema,
    componentSchemas: Map<String, Schema>,
    depth: Int = 0,
): JsonElement? {
    if (depth > 5) return null
    return generateExampleFromJsonElement(schema.schema, componentSchemas, depth)
}

private fun generateExampleFromJsonElement(
    element: JsonElement,
    componentSchemas: Map<String, Schema>,
    depth: Int,
): JsonElement? {
    if (element !is JsonObject) return null

    // Resolve $ref
    val ref = element["\$ref"]?.jsonPrimitive?.content
    if (ref != null) {
        val schemaName = ref.substringAfterLast("/")
        val resolved = componentSchemas[schemaName] ?: return null
        return generateExampleFromSchema(resolved, componentSchemas, depth + 1)
    }

    // "type" can be a string or an array (e.g., ["string", "null"] in OpenAPI 3.1+)
    val typeElement = element["type"]
    val type = when (typeElement) {
        is JsonPrimitive -> typeElement.content
        is JsonArray -> typeElement
            .filterIsInstance<JsonPrimitive>()
            .map { it.content }
            .firstOrNull { it != "null" }
        else -> null
    }

    // Check for top-level example first
    element["example"]?.let { return it }

    return when (type) {
        "object" -> {
            val properties = element["properties"] as? JsonObject ?: return JsonObject(emptyMap())
            val result = mutableMapOf<String, JsonElement>()
            properties.forEach { [propName, propValue] ->
                val propExample = generateExampleFromJsonElement(propValue, componentSchemas, depth + 1)
                if (propExample != null) {
                    result[propName] = propExample
                }
            }
            JsonObject(result)
        }
        "array" -> {
            val items = element["items"]
            if (items is JsonObject) {
                val itemExample = generateExampleFromJsonElement(items, componentSchemas, depth + 1)
                JsonArray(listOfNotNull(itemExample))
            } else {
                JsonArray(emptyList())
            }
        }
        "string" -> {
            val format = element["format"]?.jsonPrimitive?.content
            JsonPrimitive(
                when (format) {
                    "date-time" -> "2024-01-01T00:00:00Z"
                    "date" -> "2024-01-01"
                    "email" -> "user@example.com"
                    "uri", "url" -> "https://example.com"
                    "uuid" -> "00000000-0000-0000-0000-000000000000"
                    else -> {
                        // Use enum first value if available
                        val enum = element["enum"] as? JsonArray
                        enum?.firstOrNull()?.jsonPrimitive?.content ?: "string"
                    }
                }
            )
        }
        "integer" -> {
            val example = element["example"]
            if (example != null) example
            else JsonPrimitive(0)
        }
        "number" -> {
            val example = element["example"]
            if (example != null) example
            else JsonPrimitive(0.0)
        }
        "boolean" -> JsonPrimitive(false)
        else -> null
    }
}
