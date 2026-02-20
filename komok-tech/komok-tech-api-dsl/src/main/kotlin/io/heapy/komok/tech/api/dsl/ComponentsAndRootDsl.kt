package io.heapy.komok.tech.api.dsl

import kotlinx.serialization.json.JsonElement

// ============================================
// Link DSL
// ============================================

/**
 * DSL builder for [Link] object.
 *
 * The builder enforces fail-fast validation:
 * - Exactly one of `operationRef` or `operationId` must be specified
 *
 * Example usage:
 * ```kotlin
 * val link = link {
 *     operationId = "getUser"
 *     description = "Get the user that owns this resource"
 *     parameters = mapOf("userId" to "\$response.body#/userId")
 * }
 * ```
 */
class LinkBuilder {
    var operationRef: String? = null
    var operationId: String? = null
    var parameters: Map<String, String>? = null
    var requestBody: JsonElement? = null
    var description: String? = null
    var server: Server? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the server using DSL syntax.
     */
    inline fun server(block: ServerBuilder.() -> Unit) {
        server = io.heapy.komok.tech.api.dsl.server(block)
    }

    fun build(): Link {
        return Link(
            operationRef = operationRef,
            operationId = operationId,
            parameters = parameters,
            requestBody = requestBody,
            description = description,
            server = server,
            extensions = extensions,
        )
    }
}

/**
 * Creates a [Link] object using DSL syntax.
 *
 * @param block configuration block for the link
 * @return configured Link object
 * @throws IllegalArgumentException if neither or both of operationRef and operationId are provided
 */
inline fun link(block: LinkBuilder.() -> Unit): Link {
    return LinkBuilder().apply(block).build()
}

// ============================================
// Info DSL
// ============================================

/**
 * DSL builder for [Info] object.
 *
 * The builder enforces fail-fast validation:
 * - `title` is required
 * - `version` is required
 *
 * Supports nested DSL for contact and license:
 * ```kotlin
 * val info = info {
 *     title = "Petstore API"
 *     version = "1.0.0"
 *     summary = "A sample API"
 *     description = "A sample API that uses a petstore as an example"
 *     termsOfService = "https://example.com/terms/"
 *     contact {
 *         name = "API Support"
 *         url = "https://example.com/support"
 *         email = "support@example.com"
 *     }
 *     license {
 *         name = "Apache 2.0"
 *         identifier = "Apache-2.0"
 *     }
 * }
 * ```
 */
class InfoBuilder {
    var title: String? = null
    var version: String? = null
    var summary: String? = null
    var description: String? = null
    var termsOfService: String? = null
    var contact: Contact? = null
    var license: License? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures contact information using DSL syntax.
     */
    inline fun contact(block: ContactBuilder.() -> Unit) {
        contact = io.heapy.komok.tech.api.dsl.contact(block)
    }

    /**
     * Configures license information using DSL syntax.
     */
    inline fun license(block: LicenseBuilder.() -> Unit) {
        license = io.heapy.komok.tech.api.dsl.license(block)
    }

    fun build(): Info {
        val infoTitle = requireNotNull(title) {
            "Info title is required"
        }
        val infoVersion = requireNotNull(version) {
            "Info version is required"
        }
        return Info(
            title = infoTitle,
            version = infoVersion,
            summary = summary,
            description = description,
            termsOfService = termsOfService,
            contact = contact,
            license = license,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [Info] object using DSL syntax.
 *
 * @param block configuration block for the info
 * @return configured Info object
 * @throws IllegalArgumentException if title or version is not provided
 */
inline fun info(block: InfoBuilder.() -> Unit): Info {
    return InfoBuilder().apply(block).build()
}

// ============================================
// Components DSL
// ============================================

/**
 * DSL builder for a named map of schemas (used within Components).
 *
 * Example usage:
 * ```kotlin
 * schemas {
 *     "Pet" to {
 *         type = "object"
 *         required("name")
 *         properties {
 *             "name" to stringSchema()
 *         }
 *     }
 * }
 * ```
 */
class SchemasBuilder {
    @PublishedApi
    internal val schemas = mutableMapOf<String, Schema>()

    /**
     * Adds a schema using DSL syntax.
     */
    inline infix fun String.to(block: SchemaBuilder.() -> Unit) {
        schemas[this] = schema(block)
    }

    /**
     * Adds a pre-built schema.
     */
    infix fun String.to(schema: Schema) {
        schemas[this] = schema
    }

    fun build(): Map<String, Schema> = schemas.toMap()
}

/**
 * Creates a map of schemas using DSL syntax.
 */
inline fun schemas(block: SchemasBuilder.() -> Unit): Map<String, Schema> {
    return SchemasBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of responses (used within Components).
 *
 * Example usage:
 * ```kotlin
 * componentResponses {
 *     "NotFound" to {
 *         description = "Resource not found"
 *     }
 * }
 * ```
 */
class ComponentResponsesBuilder {
    @PublishedApi
    internal val responses = mutableMapOf<String, Response>()

    /**
     * Adds a response using DSL syntax.
     */
    inline infix fun String.to(block: ResponseBuilder.() -> Unit) {
        responses[this] = response(block)
    }

    /**
     * Adds a pre-built response.
     */
    infix fun String.to(response: Response) {
        responses[this] = response
    }

    fun build(): Map<String, Response> = responses.toMap()
}

/**
 * Creates a map of component responses using DSL syntax.
 */
inline fun componentResponses(block: ComponentResponsesBuilder.() -> Unit): Map<String, Response> {
    return ComponentResponsesBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of parameters (used within Components).
 */
class ComponentParametersBuilder {
    @PublishedApi
    internal val parameters = mutableMapOf<String, Parameter>()

    /**
     * Adds a parameter using DSL syntax.
     */
    inline infix fun String.to(block: ParameterBuilder.() -> Unit) {
        parameters[this] = parameter(block)
    }

    /**
     * Adds a pre-built parameter.
     */
    infix fun String.to(parameter: Parameter) {
        parameters[this] = parameter
    }

    fun build(): Map<String, Parameter> = parameters.toMap()
}

/**
 * Creates a map of component parameters using DSL syntax.
 */
inline fun componentParameters(block: ComponentParametersBuilder.() -> Unit): Map<String, Parameter> {
    return ComponentParametersBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of examples (used within Components).
 */
class ComponentExamplesBuilder {
    @PublishedApi
    internal val examples = mutableMapOf<String, Example>()

    /**
     * Adds an example using DSL syntax.
     */
    inline infix fun String.to(block: ExampleBuilder.() -> Unit) {
        examples[this] = example(block)
    }

    /**
     * Adds a pre-built example.
     */
    infix fun String.to(example: Example) {
        examples[this] = example
    }

    fun build(): Map<String, Example> = examples.toMap()
}

/**
 * Creates a map of component examples using DSL syntax.
 */
inline fun componentExamples(block: ComponentExamplesBuilder.() -> Unit): Map<String, Example> {
    return ComponentExamplesBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of request bodies (used within Components).
 */
class ComponentRequestBodiesBuilder {
    @PublishedApi
    internal val requestBodies = mutableMapOf<String, RequestBody>()

    /**
     * Adds a request body using DSL syntax.
     */
    inline infix fun String.to(block: RequestBodyBuilder.() -> Unit) {
        requestBodies[this] = requestBody(block)
    }

    /**
     * Adds a pre-built request body.
     */
    infix fun String.to(requestBody: RequestBody) {
        requestBodies[this] = requestBody
    }

    fun build(): Map<String, RequestBody> = requestBodies.toMap()
}

/**
 * Creates a map of component request bodies using DSL syntax.
 */
inline fun componentRequestBodies(block: ComponentRequestBodiesBuilder.() -> Unit): Map<String, RequestBody> {
    return ComponentRequestBodiesBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of headers (used within Components).
 */
class ComponentHeadersBuilder {
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
 * Creates a map of component headers using DSL syntax.
 */
inline fun componentHeaders(block: ComponentHeadersBuilder.() -> Unit): Map<String, Header> {
    return ComponentHeadersBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of security schemes (used within Components).
 */
class ComponentSecuritySchemesBuilder {
    @PublishedApi
    internal val securitySchemes = mutableMapOf<String, SecurityScheme>()

    /**
     * Adds a pre-built security scheme.
     */
    infix fun String.to(securityScheme: SecurityScheme) {
        securitySchemes[this] = securityScheme
    }

    /**
     * Adds an API key security scheme using DSL syntax.
     */
    inline fun String.apiKey(block: ApiKeySchemeBuilder.() -> Unit) {
        securitySchemes[this] = apiKeyScheme(block)
    }

    /**
     * Adds an HTTP security scheme using DSL syntax.
     */
    inline fun String.http(block: HttpSchemeBuilder.() -> Unit) {
        securitySchemes[this] = httpScheme(block)
    }

    /**
     * Adds a Mutual TLS security scheme using DSL syntax.
     */
    inline fun String.mutualTLS(block: MutualTLSSchemeBuilder.() -> Unit) {
        securitySchemes[this] = mutualTLSScheme(block)
    }

    /**
     * Adds an OAuth2 security scheme using DSL syntax.
     */
    inline fun String.oauth2(block: OAuth2SchemeBuilder.() -> Unit) {
        securitySchemes[this] = oauth2Scheme(block)
    }

    /**
     * Adds an OpenID Connect security scheme using DSL syntax.
     */
    inline fun String.openIdConnect(block: OpenIdConnectSchemeBuilder.() -> Unit) {
        securitySchemes[this] = openIdConnectScheme(block)
    }

    fun build(): Map<String, SecurityScheme> = securitySchemes.toMap()
}

/**
 * Creates a map of component security schemes using DSL syntax.
 */
inline fun componentSecuritySchemes(block: ComponentSecuritySchemesBuilder.() -> Unit): Map<String, SecurityScheme> {
    return ComponentSecuritySchemesBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of links (used within Components).
 */
class ComponentLinksBuilder {
    @PublishedApi
    internal val links = mutableMapOf<String, Link>()

    /**
     * Adds a link using DSL syntax.
     */
    inline infix fun String.to(block: LinkBuilder.() -> Unit) {
        links[this] = link(block)
    }

    /**
     * Adds a pre-built link.
     */
    infix fun String.to(link: Link) {
        links[this] = link
    }

    fun build(): Map<String, Link> = links.toMap()
}

/**
 * Creates a map of component links using DSL syntax.
 */
inline fun componentLinks(block: ComponentLinksBuilder.() -> Unit): Map<String, Link> {
    return ComponentLinksBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of callbacks (used within Components).
 */
class ComponentCallbacksBuilder {
    @PublishedApi
    internal val callbacks = mutableMapOf<String, Callback>()

    /**
     * Adds a callback using DSL syntax.
     */
    inline infix fun String.to(block: CallbackBuilder.() -> Unit) {
        callbacks[this] = callback(block)
    }

    /**
     * Adds a pre-built callback.
     */
    infix fun String.to(callback: Callback) {
        callbacks[this] = callback
    }

    fun build(): Map<String, Callback> = callbacks.toMap()
}

/**
 * Creates a map of component callbacks using DSL syntax.
 */
inline fun componentCallbacks(block: ComponentCallbacksBuilder.() -> Unit): Map<String, Callback> {
    return ComponentCallbacksBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of path items (used within Components).
 */
class ComponentPathItemsBuilder {
    @PublishedApi
    internal val pathItems = mutableMapOf<String, PathItem>()

    /**
     * Adds a path item using DSL syntax.
     */
    inline infix fun String.to(block: PathItemBuilder.() -> Unit) {
        pathItems[this] = pathItem(block)
    }

    /**
     * Adds a pre-built path item.
     */
    infix fun String.to(pathItem: PathItem) {
        pathItems[this] = pathItem
    }

    fun build(): Map<String, PathItem> = pathItems.toMap()
}

/**
 * Creates a map of component path items using DSL syntax.
 */
inline fun componentPathItems(block: ComponentPathItemsBuilder.() -> Unit): Map<String, PathItem> {
    return ComponentPathItemsBuilder().apply(block).build()
}

/**
 * DSL builder for a named map of media types (used within Components).
 */
class ComponentMediaTypesBuilder {
    @PublishedApi
    internal val mediaTypes = mutableMapOf<String, MediaType>()

    /**
     * Adds a media type using DSL syntax.
     */
    inline infix fun String.to(block: MediaTypeBuilder.() -> Unit) {
        mediaTypes[this] = mediaType(block)
    }

    /**
     * Adds a pre-built media type.
     */
    infix fun String.to(mediaType: MediaType) {
        mediaTypes[this] = mediaType
    }

    fun build(): Map<String, MediaType> = mediaTypes.toMap()
}

/**
 * Creates a map of component media types using DSL syntax.
 */
inline fun componentMediaTypes(block: ComponentMediaTypesBuilder.() -> Unit): Map<String, MediaType> {
    return ComponentMediaTypesBuilder().apply(block).build()
}

/**
 * DSL builder for [Components] object.
 *
 * Example usage:
 * ```kotlin
 * val components = components {
 *     schemas {
 *         "Pet" to {
 *             type = "object"
 *             required("name")
 *             properties {
 *                 "name" to stringSchema()
 *                 "tag" to stringSchema()
 *             }
 *         }
 *     }
 *     securitySchemes {
 *         "api_key".apiKey {
 *             name = "X-API-Key"
 *             location = ApiKeyLocation.HEADER
 *         }
 *         "petstore_auth".oauth2 {
 *             flows {
 *                 implicit {
 *                     authorizationUrl = "https://example.com/oauth/authorize"
 *                     scopes { "read:pets" description "Read your pets" }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
class ComponentsBuilder {
    var schemas: Map<String, Schema>? = null
    var responses: Map<String, Response>? = null
    var parameters: Map<String, Parameter>? = null
    var examples: Map<String, Example>? = null
    var requestBodies: Map<String, RequestBody>? = null
    var headers: Map<String, Header>? = null
    var securitySchemes: Map<String, SecurityScheme>? = null
    var links: Map<String, Link>? = null
    var callbacks: Map<String, Callback>? = null
    var pathItems: Map<String, PathItem>? = null
    var mediaTypes: Map<String, MediaType>? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures schemas using DSL syntax.
     */
    inline fun schemas(block: SchemasBuilder.() -> Unit) {
        schemas = io.heapy.komok.tech.api.dsl.schemas(block)
    }

    /**
     * Configures responses using DSL syntax.
     */
    inline fun responses(block: ComponentResponsesBuilder.() -> Unit) {
        responses = componentResponses(block)
    }

    /**
     * Configures parameters using DSL syntax.
     */
    inline fun parameters(block: ComponentParametersBuilder.() -> Unit) {
        parameters = componentParameters(block)
    }

    /**
     * Configures examples using DSL syntax.
     */
    inline fun examples(block: ComponentExamplesBuilder.() -> Unit) {
        examples = componentExamples(block)
    }

    /**
     * Configures request bodies using DSL syntax.
     */
    inline fun requestBodies(block: ComponentRequestBodiesBuilder.() -> Unit) {
        requestBodies = componentRequestBodies(block)
    }

    /**
     * Configures headers using DSL syntax.
     */
    inline fun headers(block: ComponentHeadersBuilder.() -> Unit) {
        headers = componentHeaders(block)
    }

    /**
     * Configures security schemes using DSL syntax.
     */
    inline fun securitySchemes(block: ComponentSecuritySchemesBuilder.() -> Unit) {
        securitySchemes = componentSecuritySchemes(block)
    }

    /**
     * Configures links using DSL syntax.
     */
    inline fun links(block: ComponentLinksBuilder.() -> Unit) {
        links = componentLinks(block)
    }

    /**
     * Configures callbacks using DSL syntax.
     */
    inline fun callbacks(block: ComponentCallbacksBuilder.() -> Unit) {
        callbacks = componentCallbacks(block)
    }

    /**
     * Configures path items using DSL syntax.
     */
    inline fun pathItems(block: ComponentPathItemsBuilder.() -> Unit) {
        pathItems = componentPathItems(block)
    }

    /**
     * Configures media types using DSL syntax.
     */
    inline fun mediaTypes(block: ComponentMediaTypesBuilder.() -> Unit) {
        mediaTypes = componentMediaTypes(block)
    }

    fun build(): Components = Components(
        schemas = schemas,
        responses = responses,
        parameters = parameters,
        examples = examples,
        requestBodies = requestBodies,
        headers = headers,
        securitySchemes = securitySchemes,
        links = links,
        callbacks = callbacks,
        pathItems = pathItems,
        mediaTypes = mediaTypes,
        extensions = extensions,
    )
}

/**
 * Creates a [Components] object using DSL syntax.
 *
 * @param block configuration block for the components
 * @return configured Components object
 */
inline fun components(block: ComponentsBuilder.() -> Unit): Components {
    return ComponentsBuilder().apply(block).build()
}

// ============================================
// Webhooks DSL
// ============================================

/**
 * DSL builder for webhooks map (Map<String, PathItem>).
 *
 * Example usage:
 * ```kotlin
 * val webhooks = webhooks {
 *     "newPet" to {
 *         post {
 *             summary = "New pet notification"
 *             responses { ok { description = "Webhook received" } }
 *         }
 *     }
 * }
 * ```
 */
class WebhooksBuilder {
    @PublishedApi
    internal val webhooks = mutableMapOf<String, PathItem>()

    /**
     * Adds a webhook using DSL syntax.
     */
    inline infix fun String.to(block: PathItemBuilder.() -> Unit) {
        webhooks[this] = pathItem(block)
    }

    /**
     * Adds a pre-built webhook path item.
     */
    infix fun String.to(pathItem: PathItem) {
        webhooks[this] = pathItem
    }

    fun build(): Map<String, PathItem> = webhooks.toMap()
}

/**
 * Creates a webhooks map using DSL syntax.
 */
inline fun webhooks(block: WebhooksBuilder.() -> Unit): Map<String, PathItem> {
    return WebhooksBuilder().apply(block).build()
}

// ============================================
// OpenAPI Root DSL
// ============================================

/**
 * DSL builder for the root [OpenAPI] object.
 *
 * The builder enforces fail-fast validation:
 * - `info` is required (title and version)
 * - At least one of `paths`, `components`, or `webhooks` must be defined
 * - `openapi` version defaults to "3.2.0"
 *
 * Example usage:
 * ```kotlin
 * val api = openAPI {
 *     info {
 *         title = "Petstore API"
 *         version = "1.0.0"
 *         description = "A sample API that uses a petstore"
 *         contact {
 *             name = "API Support"
 *             email = "support@example.com"
 *         }
 *         license {
 *             name = "Apache 2.0"
 *             identifier = "Apache-2.0"
 *         }
 *     }
 *     servers {
 *         server {
 *             url = "https://api.example.com/v1"
 *             description = "Production server"
 *         }
 *     }
 *     paths {
 *         "/pets" to {
 *             get {
 *                 summary = "List all pets"
 *                 operationId = "listPets"
 *                 responses {
 *                     ok { description = "A list of pets" }
 *                 }
 *             }
 *         }
 *     }
 *     components {
 *         securitySchemes {
 *             "api_key".apiKey {
 *                 name = "X-API-Key"
 *                 location = ApiKeyLocation.HEADER
 *             }
 *         }
 *     }
 *     security {
 *         requirement("api_key")
 *     }
 *     tags {
 *         tag {
 *             name = "pets"
 *             description = "Pet operations"
 *         }
 *     }
 * }
 * ```
 */
class OpenAPIBuilder {
    var openapi: String = OpenAPI.VERSION_3_2_0
    var info: Info? = null
    var jsonSchemaDialect: String? = null
    var servers: List<Server>? = null
    var paths: Paths? = null
    var webhooks: Map<String, PathItem>? = null
    var components: Components? = null
    var security: List<SecurityRequirement>? = null
    var tags: List<Tag>? = null
    var externalDocs: ExternalDocumentation? = null
    var self: String? = null
    var extensions: Map<String, JsonElement>? = null

    /**
     * Configures the info object using DSL syntax.
     */
    inline fun info(block: InfoBuilder.() -> Unit) {
        info = io.heapy.komok.tech.api.dsl.info(block)
    }

    /**
     * Configures servers using DSL syntax.
     */
    inline fun servers(block: ServersBuilder.() -> Unit) {
        servers = io.heapy.komok.tech.api.dsl.servers(block)
    }

    /**
     * Configures paths using DSL syntax.
     */
    inline fun paths(block: PathsBuilder.() -> Unit) {
        paths = io.heapy.komok.tech.api.dsl.paths(block)
    }

    /**
     * Configures webhooks using DSL syntax.
     */
    inline fun webhooks(block: WebhooksBuilder.() -> Unit) {
        webhooks = io.heapy.komok.tech.api.dsl.webhooks(block)
    }

    /**
     * Configures components using DSL syntax.
     */
    inline fun components(block: ComponentsBuilder.() -> Unit) {
        components = io.heapy.komok.tech.api.dsl.components(block)
    }

    /**
     * Configures security requirements using DSL syntax.
     */
    inline fun security(block: SecurityRequirementsBuilder.() -> Unit) {
        security = securityRequirements(block)
    }

    /**
     * Configures tags using DSL syntax.
     */
    inline fun tags(block: TagsBuilder.() -> Unit) {
        tags = io.heapy.komok.tech.api.dsl.tags(block)
    }

    /**
     * Configures external documentation using DSL syntax.
     */
    inline fun externalDocs(block: ExternalDocumentationBuilder.() -> Unit) {
        externalDocs = externalDocumentation(block)
    }

    fun build(): OpenAPI {
        val apiInfo = requireNotNull(info) {
            "OpenAPI info is required"
        }
        return OpenAPI(
            openapi = openapi,
            info = apiInfo,
            jsonSchemaDialect = jsonSchemaDialect,
            servers = servers,
            paths = paths,
            webhooks = webhooks,
            components = components,
            security = security,
            tags = tags,
            externalDocs = externalDocs,
            self = self,
            extensions = extensions,
        )
    }
}

/**
 * Creates an [OpenAPI] root object using DSL syntax.
 *
 * @param block configuration block for the OpenAPI document
 * @return configured OpenAPI object
 * @throws IllegalArgumentException if info is not provided
 * @throws IllegalArgumentException if none of paths, components, or webhooks is defined
 */
inline fun openAPI(block: OpenAPIBuilder.() -> Unit): OpenAPI {
    return OpenAPIBuilder().apply(block).build()
}