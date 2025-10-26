package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Holds a set of reusable objects for different aspects of the OAS.
 *
 * All objects defined within the components object will have no effect on the API
 * unless they are explicitly referenced from properties outside the components object.
 *
 * Component names must match the pattern: ^[a-zA-Z0-9._-]+$
 *
 * @property schemas An object to hold reusable Schema Objects
 * @property responses An object to hold reusable Response Objects
 * @property parameters An object to hold reusable Parameter Objects
 * @property examples An object to hold reusable Example Objects
 * @property requestBodies An object to hold reusable Request Body Objects
 * @property headers An object to hold reusable Header Objects
 * @property securitySchemes An object to hold reusable Security Scheme Objects
 * @property links An object to hold reusable Link Objects
 * @property callbacks An object to hold reusable Callback Objects
 * @property pathItems An object to hold reusable Path Item Objects
 * @property mediaTypes An object to hold reusable Media Type Objects
 * @property extensions Specification extensions (x- prefixed properties)
 *
 * @see <a href="https://spec.openapis.org/oas/v3.2#components-object">Components Object</a>
 */
@Serializable
data class Components(
    val schemas: Map<String, Schema>? = null,
    val responses: Map<String, Response>? = null,
    val parameters: Map<String, Parameter>? = null,
    val examples: Map<String, Example>? = null,
    val requestBodies: Map<String, RequestBody>? = null,
    val headers: Map<String, Header>? = null,
    val securitySchemes: Map<String, SecurityScheme>? = null,
    val links: Map<String, Link>? = null,
    val callbacks: Map<String, Callback>? = null,
    val pathItems: Map<String, PathItem>? = null,
    val mediaTypes: Map<String, MediaType>? = null,
    override val extensions: Map<String, JsonElement>? = null,
) : OpenAPIObject, SupportsExtensions {
    init {
        // Validate component names match the pattern ^[a-zA-Z0-9._-]+$
        val componentNamePattern = Regex("^[a-zA-Z0-9._-]+$")

        schemas?.keys?.forEach { validateComponentName(it, "schemas", componentNamePattern) }
        responses?.keys?.forEach { validateComponentName(it, "responses", componentNamePattern) }
        parameters?.keys?.forEach { validateComponentName(it, "parameters", componentNamePattern) }
        examples?.keys?.forEach { validateComponentName(it, "examples", componentNamePattern) }
        requestBodies?.keys?.forEach { validateComponentName(it, "requestBodies", componentNamePattern) }
        headers?.keys?.forEach { validateComponentName(it, "headers", componentNamePattern) }
        securitySchemes?.keys?.forEach { validateComponentName(it, "securitySchemes", componentNamePattern) }
        links?.keys?.forEach { validateComponentName(it, "links", componentNamePattern) }
        callbacks?.keys?.forEach { validateComponentName(it, "callbacks", componentNamePattern) }
        pathItems?.keys?.forEach { validateComponentName(it, "pathItems", componentNamePattern) }
        mediaTypes?.keys?.forEach { validateComponentName(it, "mediaTypes", componentNamePattern) }
    }

    private fun validateComponentName(name: String, componentType: String, pattern: Regex) {
        require(name.matches(pattern)) {
            "Component name '$name' in '$componentType' must match pattern ^[a-zA-Z0-9._-]+$"
        }
    }
}
