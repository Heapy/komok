# komok-tech-api-dsl-ui

> UI documentation renderer for OpenAPI specifications. Generates self-contained, interactive HTML documentation and HTTP request files from `komok-tech-api-dsl` OpenAPI model objects.

## Installation

```kotlin
dependencies {
    implementation("io.heapy.komok:komok-tech-api-dsl-ui:<version>")
    // Transitive dependency:
    // implementation("io.heapy.komok:komok-tech-api-dsl:<version>")
}
```

## Imports

```kotlin
import io.heapy.komok.tech.api.dsl.ui.renderOpenApiDoc
import io.heapy.komok.tech.api.dsl.ui.generateHttpFile
```

## Key Components

- `renderOpenApiDoc(openapi: OpenAPI): String` - Renders an OpenAPI specification into a complete, self-contained HTML page with embedded CSS and JavaScript
- `generateHttpFile(openapi: OpenAPI): String` - Generates an `.http` file for IntelliJ IDEA and VS Code REST Client with request templates for all operations

These are the only two public functions. All other components (renderer internals, styles, scripts, markdown renderer) are `internal`.

## Usage Pattern

### Generating HTML documentation

```kotlin
import io.heapy.komok.tech.api.dsl.openAPI
import io.heapy.komok.tech.api.dsl.OpenAPI
import io.heapy.komok.tech.api.dsl.ui.renderOpenApiDoc
import java.io.File

val api = openAPI {
    openapi = OpenAPI.VERSION_3_2_0
    info {
        title = "Pet Store API"
        version = "1.0.0"
        description = "A sample **Pet Store** API with Markdown support"
    }
    paths {
        "/pets" to pathItem {
            get {
                operationId = "listPets"
                summary = "List all pets"
                tags = listOf("pets")
                responses {
                    ok { description = "A list of pets" }
                }
            }
        }
    }
}

// Generate HTML documentation
val html: String = renderOpenApiDoc(api)
File("api-docs.html").writeText(html)
```

### Generating HTTP request files

```kotlin
import io.heapy.komok.tech.api.dsl.ui.generateHttpFile
import java.io.File

// Generate .http file for REST clients
val httpFile: String = generateHttpFile(api)
File("api-requests.http").writeText(httpFile)
```

## HTML Documentation Features

The generated HTML is a single self-contained file with no external dependencies:

### Layout
- Sticky header with API title, version badge, and action buttons
- Collapsible sidebar navigation with tag-based grouping
- Main content area with API info, servers, security, endpoints, and schemas

### Interactive Features
- Light/dark theme toggle with localStorage persistence
- Real-time search filtering (Ctrl/Cmd + K)
- Collapsible operations and tag groups with state persistence
- Interactive schema tree browser with expand/collapse for nested objects
- Deep linking via URL hash fragments with IntersectionObserver
- Smooth scrolling navigation
- HTTP file download button

### Content Rendering
- Markdown rendering in descriptions (via CommonMark)
- HTTP method badges with color coding (GET=blue, POST=green, PUT=orange, DELETE=red)
- Response status codes with color coding (2xx=green, 3xx=blue, 4xx=orange, 5xx=red)
- Parameters table with location, type, required indicators
- Schema tree tables with type descriptions, constraints, and `$ref` links
- Security scheme details (API Key, HTTP, OAuth 2.0, Mutual TLS, OpenID Connect)
- JSON syntax highlighting for examples

### Accessibility
- Focus-visible outlines
- Reduced-motion support
- Semantic HTML structure
- Responsive design (mobile breakpoint at 768px)

## HTTP File Features

The generated `.http` file includes:
- API title and version as file header comments
- Base URL variable from first server definition
- Request template for each operation with method, path, and query parameters
- Content-Type and Accept headers
- Request body examples generated from schemas or explicit examples
- Response status code documentation as comments
- Parameter descriptions as inline comments

## Architecture

### OpenApiDocRenderer.kt (internal)
Primary renderer that converts `OpenAPI` objects to HTML using kotlinx.html DSL. Handles all OpenAPI object types: info, servers, security schemes, paths, operations, parameters, request bodies, responses, schemas, examples, tags, and external documentation. Resolves `$ref` references to component schemas for inline rendering.

### HttpFileGenerator.kt (internal)
Generates `.http` files by iterating over all paths and operations. Generates example request bodies from schemas (resolves `$ref`, uses explicit examples, or synthesizes defaults from type/format). Supports depth-limited recursion for nested objects.

### Styles.kt (internal)
Embedded CSS with CSS custom properties for theming (light/dark mode). Provides styling for all UI components: method badges, tag groups, operations, parameter tables, schema trees, response items, security schemes. Uses CSS containment for performance.

### Scripts.kt (internal)
Embedded vanilla JavaScript for interactivity: theme toggle, search, smooth scrolling, deep linking, sidebar/operation collapse state persistence, schema tree expansion, JSON syntax highlighting, HTTP file download.

### MarkdownRenderer.kt (internal)
Converts Markdown text to kotlinx.html elements using CommonMark parser. Supports headings, text formatting, lists, links, images, code blocks, block quotes. Sanitizes HTML blocks/inline to prevent XSS.

## Dependencies

- `komok-tech-api-dsl` - OpenAPI 3.2 model classes (the input)
- `kotlinx-html` - Type-safe HTML generation DSL
- `commonmark` - Markdown parsing and rendering
- `kotlinx-serialization-json` - JSON handling for schema processing
