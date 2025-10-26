package io.heapy.komok.tech.api.dsl.ui

import io.heapy.komok.tech.api.dsl.OpenAPI
import kotlinx.html.*
import kotlinx.html.stream.createHTML

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
            openapi.security?.takeIf { it.isNotEmpty() }?.let {
                li { a(href = "#security") { +"Security" } }
            }
        }
    }

    // Endpoints section
    openapi.paths?.takeIf { it.isNotEmpty() }?.let { paths ->
        div(classes = "sidebar-section") {
            h3 { +"Endpoints" }
            ul(classes = "endpoint-list") {
                paths.forEach { (path, pathItem) ->
                    renderEndpointListItem(path, pathItem)
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
            a(href = "#${method.lowercase()}-${path.replace("/", "-").removePrefix("-")}") {
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
                // Render as markdown-like text
                +description
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

    // Paths/Endpoints section
    openapi.paths?.takeIf { it.isNotEmpty() }?.let { paths ->
        section(classes = "content-section") {
            h2 { +"Endpoints" }

            paths.forEach { (path, pathItem) ->
                renderPathItem(path, pathItem)
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

                    pre(classes = "schema-code") {
                        code {
                            // Simplified schema representation
                            +schema.toString()
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
        id = "${method.lowercase()}-${path.replace("/", "-").removePrefix("-")}"

        div(classes = "operation-header") {
            span(classes = "method-badge method-${method.lowercase()}") { +method }
            code(classes = "operation-path") { +path }

            operation.summary?.let {
                span(classes = "operation-summary") { +it }
            }
        }

        operation.description?.let {
            p(classes = "operation-description") { +it }
        }

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
                        parameters.forEach { param ->
                            tr {
                                td { code { +param.name } }
                                td { span(classes = "param-in") { +param.location.name.lowercase() } }
                                td { +"string" } // Simplified
                                td { +(if (param.required) "✓" else "-") }
                                td { +(param.description ?: "-") }
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

                requestBody.description?.let {
                    p { +it }
                }

                if (requestBody.content != null) {
                    div(classes = "content-types") {
                        requestBody.content.forEach { (contentType, _) ->
                            span(classes = "content-type-badge") { +contentType }
                        }
                    }
                }
            }
        }

        // Responses
        if (operation.responses.isNotEmpty()) {
            div(classes = "operation-section") {
                h4 { +"Responses" }

                div(classes = "responses") {
                    operation.responses.forEach { (statusCode, response) ->
                        div(classes = "response-item") {
                            span(classes = "status-code status-${statusCode.firstOrNull() ?: '2'}xx") {
                                +statusCode
                            }
                            response.summary?.let {
                                span(classes = "response-summary") { +it }
                            }
                            response.description?.let {
                                p(classes = "response-description") { +it }
                            }
                        }
                    }
                }
            }
        }
    }
}
