package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * An Example Object provides examples for the request or response.
 *
 * Note: The value fields have mutual exclusivity constraints:
 * - value and externalValue are mutually exclusive
 * - value and dataValue are mutually exclusive
 * - value and serializedValue are mutually exclusive
 * - serializedValue and externalValue are mutually exclusive
 *
 * @property summary Short description for the example
 * @property description Long description for the example
 * @property value Embedded literal example. Mutually exclusive with externalValue, dataValue, and serializedValue
 * @property dataValue Embedded data structure example. Mutually exclusive with value
 * @property serializedValue Serialized string representation. Mutually exclusive with value and externalValue
 * @property externalValue A URI that points to the literal example. Mutually exclusive with value and serializedValue
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#example-object">Example Object</a>
 */
@Serializable
data class Example(
    val summary: String? = null,
    val description: String? = null,
    val value: JsonElement? = null,
    val dataValue: JsonElement? = null,
    val serializedValue: String? = null,
    val externalValue: String? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate mutual exclusivity constraints
        require(!(value != null && externalValue != null)) {
            "Example 'value' and 'externalValue' are mutually exclusive. Only one should be specified."
        }
        require(!(value != null && dataValue != null)) {
            "Example 'value' and 'dataValue' are mutually exclusive. Only one should be specified."
        }
        require(!(value != null && serializedValue != null)) {
            "Example 'value' and 'serializedValue' are mutually exclusive. Only one should be specified."
        }
        require(!(serializedValue != null && externalValue != null)) {
            "Example 'serializedValue' and 'externalValue' are mutually exclusive. Only one should be specified."
        }
    }
}
