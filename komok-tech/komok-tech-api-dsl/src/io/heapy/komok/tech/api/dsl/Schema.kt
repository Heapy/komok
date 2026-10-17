package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The Schema Object allows the definition of input and output data types.
 *
 * In OpenAPI 3.2, schemas are JSON Schema objects that can be either:
 * - A JSON Schema object (most common)
 * - A boolean value (true = any schema valid, false = no schema valid)
 *
 * This class uses JsonElement to represent the schema, allowing for maximum flexibility
 * in representing JSON Schema Draft 2020-12 compatible schemas.
 *
 * @property schema The JSON Schema representation (can be object, boolean, or any valid JSON Schema)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#schema-object">Schema Object</a>
 * @see <a href="https://json-schema.org/draft/2020-12/json-schema-core">JSON Schema Draft 2020-12</a>
 */
@Serializable
@JvmInline
value class Schema(
    val schema: JsonElement
) : OpenAPIObject
