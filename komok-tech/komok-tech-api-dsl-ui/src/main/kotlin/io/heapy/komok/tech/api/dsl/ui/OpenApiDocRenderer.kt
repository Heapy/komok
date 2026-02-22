package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.ApiKeyLocation
import io.heapy.komok.tech.api.dsl.Content
import io.heapy.komok.tech.api.dsl.Direct
import io.heapy.komok.tech.api.dsl.ExternalDocumentation
import io.heapy.komok.tech.api.dsl.MediaType
import io.heapy.komok.tech.api.dsl.OAuthFlow
import io.heapy.komok.tech.api.dsl.OpenAPI
import io.heapy.komok.tech.api.dsl.Reference
import io.heapy.komok.tech.api.dsl.Schema
import io.heapy.komok.tech.api.dsl.SecuritySchemeType
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private val prettyJson = Json { prettyPrint = true }

/**
 * Extracts a `$ref` value from a schema if it is a simple reference object.
 * Returns null if the schema is not a `$ref`.
 */
private fun Schema.refOrNull(): String? {
    val obj = schema as? JsonObject ?: return null
    if (obj.size == 1 && obj.containsKey("\$ref")) {
        return obj["\$ref"]?.jsonPrimitive?.content
    }
    return null
}

/**
 * Extracts the short name from a `$ref` path like `#/components/schemas/Pet` -> `Pet`.
 */
private fun refShortName(ref: String): String =
    ref.substringAfterLast("/")

/**
 * Returns the CSS class suffix for a response status code.
 * Maps numeric codes to their Nxx class, and "default" to "default".
 */
private fun statusCodeCssClass(statusCode: String): String {
    val first = statusCode.firstOrNull()
    return if (first != null && first.isDigit()) "status-${first}xx" else "status-default"
}

/**
 * Renders an OpenAPI document as a self-contained HTML page with embedded CSS and JavaScript.
 *
 * This function generates a web-server-agnostic HTML documentation page for an OpenAPI specification.
 * The generated page includes:
 * - Semantic HTML structure
 * - Embedded responsive CSS with dark/light theme support
 * - Interactive JavaScript using Preact for collapsible sections, search, and navigation
 * - Performance-optimized rendering
 *
 * @param openapi The OpenAPI document to render
 * @return A complete HTML string ready to be served
 */
fun renderOpenApiDoc(openapi: OpenAPI): String = createHTML().html {
    lang = "en"

    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title("${openapi.info.title} - API Documentation")

        // Embedded CSS
        style {
            unsafe {
                +CSS_STYLES
            }
        }
    }

    body {
        attributes["data-theme"] = "light"

        // Main container
        div(classes = "api-doc-container") {
            id = "app"

            // Header
            renderHeader(openapi)

            // Main content area with sidebar
            div(classes = "main-content") {
                // Sidebar navigation
                nav(classes = "sidebar") {
                    id = "sidebar"
                    renderSidebar(openapi)
                }

                // Content area
                main(classes = "content") {
                    id = "content"
                    renderContent(openapi)
                }
            }
        }

        // Embedded JavaScript
        script {
            type = "module"
            unsafe {
                +JAVASCRIPT_CODE
            }
        }
    }
}

/**
 * Renders the header section with API title, version, and theme toggle.
 */
private fun FlowContent.renderHeader(openapi: OpenAPI) {
    header(classes = "header") {
        div(classes = "header-content") {
            div(classes = "header-left") {
                h1 { +openapi.info.title }
                span(classes = "version-badge") { +"v${openapi.info.version}" }
                openapi.info.summary?.let { summary ->
                    p(classes = "api-summary") { +summary }
                }
            }

            div(classes = "header-right") {
                // HTTP file download button
                openapi.paths?.takeIf { it.isNotEmpty() }?.let {
                    val httpFileContent = generateHttpFile(openapi)
                    button(classes = "download-http-file") {
                        id = "download-http-file"
                        attributes["aria-label"] = "Download HTTP file"
                        attributes["data-openapi-title"] = openapi.info.title
                        attributes["data-http-content"] = httpFileContent
                        +"Download .http"
                    }
                }

                // Theme toggle button
                button(classes = "theme-toggle") {
                    id = "theme-toggle"
                    attributes["aria-label"] = "Toggle theme"
                    span(classes = "theme-icon") { +"🌙" }
                }

                // Search button
                button(classes = "search-toggle") {
                    id = "search-toggle"
                    attributes["aria-label"] = "Search"
                    +"🔍"
                }
            }
        }

        // Search bar (initially hidden)
        div(classes = "search-container") {
            id = "search-container"
            attributes["style"] = "display: none;"

            input(type = InputType.search, classes = "search-input") {
                id = "search-input"
                placeholder = "Search endpoints..."
                attributes["aria-label"] = "Search endpoints"
            }
        }
    }
}

/**
 * Renders the sidebar navigation with endpoint list.
 */
private fun FlowContent.renderSidebar(openapi: OpenAPI) {
    // API Info section
    div(classes = "sidebar-section") {
        h3 { +"Overview" }
        ul {
            li { a(href = "#info") { +"Information" } }
            openapi.servers?.takeIf { it.isNotEmpty() }?.let {
                li { a(href = "#servers") { +"Servers" } }
            }
            openapi.components?.securitySchemes?.takeIf { it.isNotEmpty() }?.let {
                li { a(href = "#security") { +"Security" } }
            }
        }
    }

    // Endpoints section - grouped by tags
    openapi.paths?.takeIf { it.isNotEmpty() }?.let { paths ->
        div(classes = "sidebar-section") {
            h3 { +"Endpoints" }

            // Group operations by tag for sidebar
            val operationsByTag = mutableMapOf<String?, MutableList<Pair<String, io.heapy.komok.tech.api.dsl.PathItem>>>()

            paths.forEach { (path, pathItem) ->
                val operations = listOfNotNull(
                    pathItem.get,
                    pathItem.post,
                    pathItem.put,
                    pathItem.delete,
                    pathItem.patch,
                    pathItem.options,
                    pathItem.head,
                    pathItem.trace
                )

                operations.forEach { operation ->
                    val tag = operation.tags?.firstOrNull()
                    operationsByTag.getOrPut(tag) { mutableListOf() }.add(path to pathItem)
                }
            }

            // Remove duplicates and render
            val tagOrder = openapi.tags?.map { it.name } ?: emptyList()
            val sortedTags = operationsByTag.keys.sortedBy { tag ->
                val index = tagOrder.indexOf(tag)
                if (index >= 0) index else Int.MAX_VALUE
            }

            sortedTags.forEach { tag ->
                div(classes = "sidebar-tag-group") {
                    if (tag != null) {
                        attributes["data-tag"] = tag
                    }

                    div(classes = "sidebar-tag-header") {
                        button(classes = "sidebar-tag-toggle") {
                            attributes["type"] = "button"
                            attributes["aria-expanded"] = "true"
                            attributes["aria-label"] = "Toggle ${tag ?: "endpoints"} group"
                            span(classes = "toggle-icon") { +"▼" }
                        }
                        if (tag != null) {
                            a(href = "#tag-$tag", classes = "sidebar-tag-name") { +tag }
                        }
                    }

                    ul(classes = "endpoint-list") {
                        operationsByTag[tag]?.distinctBy { it.first }?.forEach { (path, pathItem) ->
                            renderEndpointListItem(path, pathItem)
                        }
                    }
                }
            }
        }
    }

    // Schemas section
    openapi.components?.schemas?.takeIf { it.isNotEmpty() }?.let { schemas ->
        div(classes = "sidebar-section") {
            h3 { +"Schemas" }
            ul {
                schemas.keys.sorted().forEach { schemaName ->
                    li { a(href = "#schema-$schemaName") { +schemaName } }
                }
            }
        }
    }
}

/**
 * Sanitizes a path to create a valid HTML ID.
 * Removes slashes, curly braces, and other special characters.
 */
private fun sanitizePathForId(path: String): String {
    return path
        .replace("/", "-")
        .replace("{", "")
        .replace("}", "")
        .removePrefix("-")
}

/**
 * Renders an endpoint list item in the sidebar.
 */
private fun UL.renderEndpointListItem(path: String, pathItem: io.heapy.komok.tech.api.dsl.PathItem) {
    val operations = listOfNotNull(
        pathItem.get?.let { "GET" to it },
        pathItem.post?.let { "POST" to it },
        pathItem.put?.let { "PUT" to it },
        pathItem.delete?.let { "DELETE" to it },
        pathItem.patch?.let { "PATCH" to it },
        pathItem.options?.let { "OPTIONS" to it },
        pathItem.head?.let { "HEAD" to it },
        pathItem.trace?.let { "TRACE" to it }
    )

    operations.forEach { (method, operation) ->
        li(classes = "endpoint-item") {
            a(href = "#${method.lowercase()}-${sanitizePathForId(path)}") {
                span(classes = "method-badge method-${method.lowercase()}") { +method }
                span(classes = "endpoint-path") { +path }
            }
        }
    }
}

/**
 * Renders the main content area.
 */
private fun FlowContent.renderContent(openapi: OpenAPI) {
    // Info section
    section(classes = "content-section") {
        id = "info"
        h2 { +"API Information" }

        openapi.info.description?.let { description ->
            div(classes = "description") {
                markdown(description)
            }
        }

        if (openapi.info.termsOfService != null || openapi.info.contact != null || openapi.info.license != null) {
            div(classes = "info-details") {
                openapi.info.termsOfService?.let {
                    p {
                        strong { +"Terms of Service: " }
                        a(href = it, target = "_blank") { +it }
                    }
                }

                openapi.info.contact?.let { contact ->
                    p {
                        strong { +"Contact: " }
                        contact.name?.let { +"$it " }
                        contact.email?.let { a(href = "mailto:$it") { +it } }
                        contact.url?.let { +" - "; a(href = it, target = "_blank") { +it } }
                    }
                }

                openapi.info.license?.let { license ->
                    p {
                        strong { +"License: " }
                        if (license.url != null) {
                            a(href = license.url, target = "_blank") { +license.name }
                        } else {
                            +license.name
                        }
                    }
                }
            }
        }

        // Root external documentation
        openapi.externalDocs?.let { renderExternalDocs(it) }
    }

    // Servers section
    openapi.servers?.takeIf { it.isNotEmpty() }?.let { servers ->
        section(classes = "content-section") {
            id = "servers"
            h2 { +"Servers" }

            div(classes = "servers-list") {
                servers.forEach { server ->
                    div(classes = "server-item") {
                        code(classes = "server-url") { +server.url }
                        server.description?.let {
                            p(classes = "server-description") { +it }
                        }
                    }
                }
            }
        }
    }

    // Security schemes section
    openapi.components?.securitySchemes?.takeIf { it.isNotEmpty() }?.let { securitySchemes ->
        renderSecuritySchemes(securitySchemes, openapi.security)
    }

    // Paths/Endpoints section - grouped by tags
    openapi.paths?.takeIf { it.isNotEmpty() }?.let { paths ->
        section(classes = "content-section") {
            h2 { +"Endpoints" }

            // Group operations by tag
            val operationsByTag = mutableMapOf<String?, MutableList<Triple<String, String, io.heapy.komok.tech.api.dsl.Operation>>>()

            paths.forEach { (path, pathItem) ->
                val operations = listOfNotNull(
                    pathItem.get?.let { Triple("GET", path, it) },
                    pathItem.post?.let { Triple("POST", path, it) },
                    pathItem.put?.let { Triple("PUT", path, it) },
                    pathItem.delete?.let { Triple("DELETE", path, it) },
                    pathItem.patch?.let { Triple("PATCH", path, it) },
                    pathItem.options?.let { Triple("OPTIONS", path, it) },
                    pathItem.head?.let { Triple("HEAD", path, it) },
                    pathItem.trace?.let { Triple("TRACE", path, it) }
                )

                operations.forEach { (method, p, operation) ->
                    val tag = operation.tags?.firstOrNull()
                    operationsByTag.getOrPut(tag) { mutableListOf() }.add(Triple(method, p, operation))
                }
            }

            // Render operations grouped by tag
            val tagOrder = openapi.tags?.map { it.name } ?: emptyList()
            val sortedTags = operationsByTag.keys.sortedBy { tag ->
                val index = tagOrder.indexOf(tag)
                if (index >= 0) index else Int.MAX_VALUE
            }

            sortedTags.forEach { tag ->
                val tagInfo = openapi.tags?.find { it.name == tag }

                div(classes = "tag-group") {
                    if (tag != null) {
                        h3(classes = "tag-name") {
                            id = "tag-$tag"
                            +tag
                        }
                        tagInfo?.description?.let { desc ->
                            div(classes = "tag-description") {
                                markdown(desc)
                            }
                        }
                        tagInfo?.externalDocs?.let { renderExternalDocs(it) }
                    } else {
                        h3(classes = "tag-name") { +"Untagged" }
                    }

                    operationsByTag[tag]?.forEach { (method, path, operation) ->
                        renderOperation(method, path, operation)
                    }
                }
            }
        }
    }

    // Schemas section
    openapi.components?.schemas?.takeIf { it.isNotEmpty() }?.let { schemas ->
        section(classes = "content-section") {
            h2 { +"Schemas" }

            schemas.entries.sortedBy { it.key }.forEach { (name, schema) ->
                div(classes = "schema-item") {
                    id = "schema-$name"
                    h3 { +name }

                    val obj = schema.schema as? JsonObject
                    val typeValue = (obj?.get("type") as? JsonPrimitive)?.content
                    if (obj != null && typeValue == "object" && obj.containsKey("properties")) {
                        renderSchemaTree(obj, schemas)
                    } else {
                        pre(classes = "schema-code") {
                            code(classes = "language-json") {
                                +prettyJson.encodeToString(JsonElement.serializer(), schema.schema)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an external documentation link.
 */
private fun FlowContent.renderExternalDocs(externalDocs: ExternalDocumentation) {
    a(href = externalDocs.url, target = "_blank", classes = "external-docs-link") {
        +(externalDocs.description ?: "External Documentation")
    }
}

/**
 * Renders the security schemes section.
 */
private fun FlowContent.renderSecuritySchemes(
    securitySchemes: Map<String, io.heapy.komok.tech.api.dsl.SecurityScheme>,
    globalSecurity: List<Map<String, List<String>>>?,
) {
    section(classes = "content-section") {
        id = "security"
        h2 { +"Security" }

        // Global security requirements
        globalSecurity?.takeIf { it.isNotEmpty() }?.let { requirements ->
            div(classes = "security-global") {
                h3 { +"Global Security Requirements" }
                p { +"The following security schemes are required globally:" }
                ul {
                    requirements.forEach { requirement ->
                        requirement.forEach { (schemeName, scopes) ->
                            li {
                                strong { +schemeName }
                                if (scopes.isNotEmpty()) {
                                    +" (scopes: ${scopes.joinToString(", ")})"
                                }
                            }
                        }
                    }
                }
            }
        }

        // Individual security schemes
        securitySchemes.forEach { (name, scheme) ->
            div(classes = "security-scheme") {
                div(classes = "security-scheme-header") {
                    h3 { +name }
                    span(classes = "security-scheme-type") {
                        +when (scheme.type) {
                            SecuritySchemeType.API_KEY -> "API Key"
                            SecuritySchemeType.HTTP -> "HTTP"
                            SecuritySchemeType.MUTUAL_TLS -> "Mutual TLS"
                            SecuritySchemeType.OAUTH2 -> "OAuth 2.0"
                            SecuritySchemeType.OPEN_ID_CONNECT -> "OpenID Connect"
                        }
                    }
                    if (scheme.deprecated) {
                        span(classes = "deprecated-badge") { +"Deprecated" }
                    }
                }

                scheme.description?.let { desc ->
                    div(classes = "security-scheme-description") {
                        markdown(desc)
                    }
                }

                // Type-specific details
                when (scheme.type) {
                    SecuritySchemeType.API_KEY -> {
                        table(classes = "security-details-table") {
                            tbody {
                                tr {
                                    td { strong { +"Parameter Name" } }
                                    td { code { +(scheme.name ?: "") } }
                                }
                                tr {
                                    td { strong { +"Location" } }
                                    td {
                                        +when (scheme.location) {
                                            ApiKeyLocation.QUERY -> "query"
                                            ApiKeyLocation.HEADER -> "header"
                                            ApiKeyLocation.COOKIE -> "cookie"
                                            null -> ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SecuritySchemeType.HTTP -> {
                        table(classes = "security-details-table") {
                            tbody {
                                tr {
                                    td { strong { +"Scheme" } }
                                    td { code { +(scheme.scheme ?: "") } }
                                }
                                scheme.bearerFormat?.let { format ->
                                    tr {
                                        td { strong { +"Bearer Format" } }
                                        td { code { +format } }
                                    }
                                }
                            }
                        }
                    }
                    SecuritySchemeType.MUTUAL_TLS -> {
                        // No additional details beyond description
                    }
                    SecuritySchemeType.OAUTH2 -> {
                        scheme.flows?.let { flows ->
                            div(classes = "security-flows") {
                                flows.implicit?.let { renderOAuthFlow("Implicit", it) }
                                flows.password?.let { renderOAuthFlow("Password", it) }
                                flows.clientCredentials?.let { renderOAuthFlow("Client Credentials", it) }
                                flows.authorizationCode?.let { renderOAuthFlow("Authorization Code", it) }
                                flows.deviceAuthorization?.let { renderOAuthFlow("Device Authorization", it) }
                            }
                        }
                    }
                    SecuritySchemeType.OPEN_ID_CONNECT -> {
                        table(classes = "security-details-table") {
                            tbody {
                                tr {
                                    td { strong { +"Discovery URL" } }
                                    td {
                                        a(href = scheme.openIdConnectUrl ?: "", target = "_blank") {
                                            +(scheme.openIdConnectUrl ?: "")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an OAuth flow section.
 */
private fun FlowContent.renderOAuthFlow(flowName: String, flow: OAuthFlow) {
    div(classes = "security-flow") {
        h4 { +flowName }

        table(classes = "security-details-table") {
            tbody {
                when (flow) {
                    is OAuthFlow.Implicit -> {
                        tr {
                            td { strong { +"Authorization URL" } }
                            td { a(href = flow.authorizationUrl, target = "_blank") { +flow.authorizationUrl } }
                        }
                    }
                    is OAuthFlow.Password -> {
                        tr {
                            td { strong { +"Token URL" } }
                            td { a(href = flow.tokenUrl, target = "_blank") { +flow.tokenUrl } }
                        }
                    }
                    is OAuthFlow.ClientCredentials -> {
                        tr {
                            td { strong { +"Token URL" } }
                            td { a(href = flow.tokenUrl, target = "_blank") { +flow.tokenUrl } }
                        }
                    }
                    is OAuthFlow.AuthorizationCode -> {
                        tr {
                            td { strong { +"Authorization URL" } }
                            td { a(href = flow.authorizationUrl, target = "_blank") { +flow.authorizationUrl } }
                        }
                        tr {
                            td { strong { +"Token URL" } }
                            td { a(href = flow.tokenUrl, target = "_blank") { +flow.tokenUrl } }
                        }
                    }
                    is OAuthFlow.DeviceAuthorization -> {
                        tr {
                            td { strong { +"Device Authorization URL" } }
                            td { a(href = flow.deviceAuthorizationUrl, target = "_blank") { +flow.deviceAuthorizationUrl } }
                        }
                        tr {
                            td { strong { +"Token URL" } }
                            td { a(href = flow.tokenUrl, target = "_blank") { +flow.tokenUrl } }
                        }
                    }
                }
                flow.refreshUrl?.let { refreshUrl ->
                    tr {
                        td { strong { +"Refresh URL" } }
                        td { a(href = refreshUrl, target = "_blank") { +refreshUrl } }
                    }
                }
            }
        }

        if (flow.scopes.isNotEmpty()) {
            table(classes = "security-scopes-table") {
                thead {
                    tr {
                        th { +"Scope" }
                        th { +"Description" }
                    }
                }
                tbody {
                    flow.scopes.forEach { (scope, description) ->
                        tr {
                            td { code { +scope } }
                            td { +description }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a schema - either as a $ref link or as pretty-printed JSON.
 */
private fun FlowContent.renderSchema(schema: Schema) {
    val ref = schema.refOrNull()
    if (ref != null) {
        val name = refShortName(ref)
        a(href = "#schema-$name", classes = "schema-ref") {
            +name
        }
    } else {
        pre(classes = "schema-code") {
            code(classes = "language-json") {
                +prettyJson.encodeToString(JsonElement.serializer(), schema.schema)
            }
        }
    }
}

/**
 * Extracts the type description from a JSON schema element.
 * Returns a human-readable type string (e.g., "string", "integer", "array[string]").
 */
private fun schemaTypeDescription(element: JsonObject): String {
    val ref = element["\$ref"]?.jsonPrimitive?.content
    if (ref != null) return refShortName(ref)

    val typeElement = element["type"]
    val type = when (typeElement) {
        is JsonPrimitive -> typeElement.content
        is JsonArray -> typeElement
            .filterIsInstance<JsonPrimitive>()
            .joinToString(" | ") { it.content }
        else -> null
    }

    val format = element["format"]?.jsonPrimitive?.content

    return when (type) {
        "array" -> {
            val items = element["items"] as? JsonObject
            val itemType = if (items != null) schemaTypeDescription(items) else "any"
            "array[$itemType]"
        }
        else -> {
            val base = type ?: "any"
            if (format != null) "$base ($format)" else base
        }
    }
}

/**
 * Renders an object schema as an interactive tree table with expand/collapse.
 * Properties are shown in a structured format with name, type, required flag, and description.
 * Nested objects and $ref types are expandable.
 */
private fun FlowContent.renderSchemaTree(
    schemaObj: JsonObject,
    allSchemas: Map<String, Schema>,
) {
    val requiredSet = (schemaObj["required"] as? JsonArray)
        ?.filterIsInstance<JsonPrimitive>()
        ?.map { it.content }
        ?.toSet()
        ?: emptySet()

    val properties = schemaObj["properties"] as? JsonObject ?: return

    div(classes = "schema-tree") {
        table(classes = "schema-tree-table") {
            thead {
                tr {
                    th { +"Name" }
                    th { +"Type" }
                    th { +"Required" }
                    th { +"Description" }
                }
            }
            tbody {
                renderSchemaProperties(properties, requiredSet, allSchemas, depth = 0)
            }
        }
    }
}

/**
 * Renders schema properties as table rows, recursively rendering nested objects.
 */
private fun TBODY.renderSchemaProperties(
    properties: JsonObject,
    requiredSet: Set<String>,
    allSchemas: Map<String, Schema>,
    depth: Int,
) {
    properties.forEach { (propName, propValue) ->
        val propObj = propValue as? JsonObject ?: return@forEach
        val isRequired = propName in requiredSet
        val typeDesc = schemaTypeDescription(propObj)
        val description = (propObj["description"] as? JsonPrimitive)?.content ?: ""
        val ref = propObj["\$ref"]?.jsonPrimitive?.content

        // Check if this property has nested properties (inline object)
        val nestedProps = propObj["properties"] as? JsonObject
        // Check if this is an array of objects
        val arrayItemProps = (propObj["items"] as? JsonObject)?.get("properties") as? JsonObject
        // Check if $ref resolves to an object schema
        val refSchemaProps = if (ref != null) {
            val refName = refShortName(ref)
            val refSchema = allSchemas[refName]?.schema as? JsonObject
            if ((refSchema?.get("type") as? JsonPrimitive)?.content == "object") {
                refSchema["properties"] as? JsonObject
            } else null
        } else null

        val hasChildren = nestedProps != null || arrayItemProps != null || refSchemaProps != null

        tr(classes = "schema-prop-row${if (hasChildren) " schema-expandable" else ""}") {
            attributes["data-depth"] = depth.toString()
            if (hasChildren) {
                attributes["data-expanded"] = "false"
            }

            td {
                span(classes = "schema-prop-indent") {
                    if (depth > 0) {
                        attributes["style"] = "padding-left: ${depth * 20}px"
                    }
                    if (hasChildren) {
                        span(classes = "schema-expand-icon") { +"▶" }
                    }
                    code { +propName }
                }
            }
            td {
                if (ref != null) {
                    a(href = "#schema-${refShortName(ref)}", classes = "schema-ref") {
                        +typeDesc
                    }
                } else {
                    span(classes = "schema-type") { +typeDesc }
                }
            }
            td { +(if (isRequired) "Yes" else "No") }
            td {
                +description
                // Show constraints
                val constraints = buildList {
                    (propObj["minLength"] as? JsonPrimitive)?.content?.let { add("minLength: $it") }
                    (propObj["maxLength"] as? JsonPrimitive)?.content?.let { add("maxLength: $it") }
                    (propObj["minimum"] as? JsonPrimitive)?.content?.let { add("min: $it") }
                    (propObj["maximum"] as? JsonPrimitive)?.content?.let { add("max: $it") }
                    (propObj["pattern"] as? JsonPrimitive)?.content?.let { add("pattern: $it") }
                    (propObj["enum"] as? JsonArray)?.let { e ->
                        val values = e.filterIsInstance<JsonPrimitive>().joinToString(", ") { it.content }
                        if (values.isNotEmpty()) add("enum: $values")
                    }
                }
                if (constraints.isNotEmpty()) {
                    span(classes = "schema-constraints") {
                        +" (${constraints.joinToString(", ")})"
                    }
                }
            }
        }

        // Render nested children (hidden by default, toggled by JS)
        if (hasChildren) {
            val childProps = nestedProps ?: arrayItemProps ?: refSchemaProps!!
            val childRequired = when {
                nestedProps != null -> (propObj["required"] as? JsonArray)
                    ?.filterIsInstance<JsonPrimitive>()?.map { it.content }?.toSet() ?: emptySet()
                arrayItemProps != null -> ((propObj["items"] as? JsonObject)?.get("required") as? JsonArray)
                    ?.filterIsInstance<JsonPrimitive>()?.map { it.content }?.toSet() ?: emptySet()
                refSchemaProps != null -> {
                    val refName = refShortName(ref!!)
                    val refSchema = allSchemas[refName]?.schema as? JsonObject
                    (refSchema?.get("required") as? JsonArray)
                        ?.filterIsInstance<JsonPrimitive>()?.map { it.content }?.toSet() ?: emptySet()
                }
                else -> emptySet()
            }
            renderSchemaProperties(childProps, childRequired, allSchemas, depth + 1)
        }
    }
}

/**
 * Renders content (media types) grouped by schema to avoid duplication.
 * If multiple content types share the same schema, they are shown together.
 */
private fun FlowContent.renderContent(content: Content, showSchemaHeading: Boolean = true) {
    // Group content types by their schema JSON string to deduplicate
    data class ContentGroup(
        val contentTypes: MutableList<String>,
        val mediaType: MediaType,
    )

    val groups = mutableListOf<ContentGroup>()
    content.forEach { (contentType, mediaType) ->
        val schemaKey = mediaType.schema?.let {
            prettyJson.encodeToString(JsonElement.serializer(), it.schema)
        } ?: ""
        val existing = groups.find { group ->
            val existingKey = group.mediaType.schema?.let {
                prettyJson.encodeToString(JsonElement.serializer(), it.schema)
            } ?: ""
            existingKey == schemaKey
                && group.mediaType.example == mediaType.example
                && group.mediaType.examples == mediaType.examples
        }
        if (existing != null) {
            existing.contentTypes.add(contentType)
        } else {
            groups.add(ContentGroup(mutableListOf(contentType), mediaType))
        }
    }

    groups.forEach { group ->
        div(classes = "content-type-section") {
            div(classes = "content-types") {
                group.contentTypes.forEach { ct ->
                    span(classes = "content-type-badge") { +ct }
                }
            }

            group.mediaType.schema?.let { schema ->
                div(classes = "schema-display") {
                    if (showSchemaHeading) {
                        h5 { +"Schema" }
                    }
                    renderSchema(schema)
                }
            }

            group.mediaType.example?.let { example ->
                div(classes = "example-display") {
                    h5 { +"Example" }
                    pre(classes = "schema-code") {
                        code(classes = "language-json") {
                            +prettyJson.encodeToString(JsonElement.serializer(), example)
                        }
                    }
                }
            }

            group.mediaType.examples?.takeIf { it.isNotEmpty() }?.let { examples ->
                div(classes = "examples-display") {
                    h5 { +"Examples" }
                    examples.forEach { (exampleName, example) ->
                        div(classes = "named-example") {
                            h6 { +exampleName }
                            example.summary?.let { p { em { +it } } }
                            example.description?.let { desc -> div { markdown(desc) } }
                            example.value?.let { value ->
                                pre(classes = "schema-code") {
                                    code(classes = "language-json") {
                                        +prettyJson.encodeToString(JsonElement.serializer(), value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a path item with all its operations.
 */
private fun FlowContent.renderPathItem(path: String, pathItem: io.heapy.komok.tech.api.dsl.PathItem) {
    val operations = listOfNotNull(
        pathItem.get?.let { "GET" to it },
        pathItem.post?.let { "POST" to it },
        pathItem.put?.let { "PUT" to it },
        pathItem.delete?.let { "DELETE" to it },
        pathItem.patch?.let { "PATCH" to it },
        pathItem.options?.let { "OPTIONS" to it },
        pathItem.head?.let { "HEAD" to it },
        pathItem.trace?.let { "TRACE" to it }
    )

    operations.forEach { (method, operation) ->
        renderOperation(method, path, operation)
    }
}

/**
 * Renders a single operation.
 */
private fun FlowContent.renderOperation(method: String, path: String, operation: io.heapy.komok.tech.api.dsl.Operation) {
    div(classes = "operation") {
        id = "${method.lowercase()}-${sanitizePathForId(path)}"

        div(classes = "operation-header") {
            button(classes = "operation-toggle") {
                attributes["type"] = "button"
                attributes["aria-label"] = "Toggle operation details"
                span(classes = "operation-toggle-icon") { +"▼" }
            }
            span(classes = "method-badge method-${method.lowercase()}") { +method }
            code(classes = "operation-path") { +path }

            operation.summary?.let {
                span(classes = "operation-summary") { +it }
            }
        }

        // Operation details - collapsible
        div(classes = "operation-details") {
            operation.description?.let { desc ->
                div(classes = "operation-description") {
                    markdown(desc)
                }
            }

            // External documentation
            operation.externalDocs?.let { renderExternalDocs(it) }

            // Parameters
            operation.parameters?.takeIf { it.isNotEmpty() }?.let { parameters ->
                div(classes = "operation-section") {
                    h4 { +"Parameters" }

                    table(classes = "params-table") {
                        thead {
                            tr {
                                th { +"Name" }
                                th { +"In" }
                                th { +"Type" }
                                th { +"Required" }
                                th { +"Description" }
                            }
                        }
                        tbody {
                            parameters.forEach { paramRef ->
                                tr {
                                    when (paramRef) {
                                        is Direct -> {
                                            val param = paramRef.value
                                            td { code { +param.name } }
                                            td { span(classes = "param-in") { +param.location.name.lowercase() } }
                                            td { +"string" } // Simplified
                                            td { +(if (param.required) "✓" else "-") }
                                            td {
                                                param.description?.let { desc ->
                                                    markdown(desc)
                                                } ?: run { +"-" }
                                            }
                                        }
                                        is Reference -> {
                                            td {
                                                colSpan = "5"
                                                code { +paramRef.ref }
                                                paramRef.description?.let { desc ->
                                                    +" - "
                                                    +desc
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Request Body
            operation.requestBody?.let { requestBody ->
                div(classes = "operation-section") {
                    h4 { +"Request Body" }

                    requestBody.description?.let { desc ->
                        div {
                            markdown(desc)
                        }
                    }

                    if (requestBody.required) {
                        span(classes = "required-badge") { +"Required" }
                    }

                    renderContent(requestBody.content)
                }
            }

            // Responses
            if (operation.responses.isNotEmpty()) {
                div(classes = "operation-section") {
                    h4 { +"Responses" }

                    div(classes = "responses") {
                        operation.responses.forEach { (statusCode, response) ->
                            div(classes = "response-item") {
                                span(classes = "status-code ${statusCodeCssClass(statusCode)}") {
                                    +statusCode
                                }
                                response.summary?.let {
                                    span(classes = "response-summary") { +it }
                                }
                                response.description?.let { desc ->
                                    div(classes = "response-description") {
                                        markdown(desc)
                                    }
                                }

                                // Response content types and schemas
                                response.content?.let { responseContent ->
                                    renderContent(responseContent, showSchemaHeading = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
