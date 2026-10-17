package io.heapy.komok.tech.api.dsl

/**
 * Lists the required security schemes to execute an operation.
 *
 * When a list of Security Requirement Objects is defined on the OpenAPI Object or Operation Object,
 * only one of the Security Requirement Objects in the list needs to be satisfied to authorize the request.
 *
 * Each named security scheme must be a key in the Security Schemes under the Components Object.
 *
 * For security schemes that use OAuth2 or OpenID Connect, the value is a list of scope names required
 * for the execution. For other security scheme types, the array may be empty.
 *
 * Example:
 * ```kotlin
 * // OAuth2 security requirement with scopes
 * val oauth2Requirement = securityRequirement(
 *     "petstore_auth" to listOf("write:pets", "read:pets")
 * )
 *
 * // API Key security requirement (no scopes)
 * val apiKeyRequirement = securityRequirement(
 *     "api_key" to emptyList()
 * )
 * ```
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#security-requirement-object">Security Requirement Object</a>
 */
typealias SecurityRequirement = Map<String, List<String>>

/**
 * Creates a SecurityRequirement from vararg pairs.
 *
 * @param pairs key-value pairs where keys are security scheme names and values are lists of scopes
 * @return map representing the security requirement
 */
fun securityRequirement(vararg pairs: Pair<String, List<String>>): SecurityRequirement {
    return mapOf(*pairs)
}
