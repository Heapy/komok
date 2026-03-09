# komok-tech-api-dsl

> Kotlin DSL and type-safe data model library for authoring OpenAPI 3.2 specifications programmatically, with Kotlinx Serialization support, schema inference from Kotlin types, and JSON Pointer reference resolution.

## Installation

```kotlin
dependencies {
    implementation("io.heapy.komok:komok-tech-api-dsl:<version>")
}
```

## Imports

```kotlin
// Core model classes
import io.heapy.komok.tech.api.dsl.OpenAPI
import io.heapy.komok.tech.api.dsl.Info
import io.heapy.komok.tech.api.dsl.Server
import io.heapy.komok.tech.api.dsl.PathItem
import io.heapy.komok.tech.api.dsl.Operation
import io.heapy.komok.tech.api.dsl.Parameter
import io.heapy.komok.tech.api.dsl.RequestBody
import io.heapy.komok.tech.api.dsl.Response
import io.heapy.komok.tech.api.dsl.Header
import io.heapy.komok.tech.api.dsl.MediaType
import io.heapy.komok.tech.api.dsl.Schema
import io.heapy.komok.tech.api.dsl.Example
import io.heapy.komok.tech.api.dsl.Components
import io.heapy.komok.tech.api.dsl.SecurityScheme
import io.heapy.komok.tech.api.dsl.Link
import io.heapy.komok.tech.api.dsl.Tag
import io.heapy.komok.tech.api.dsl.Contact
import io.heapy.komok.tech.api.dsl.License
import io.heapy.komok.tech.api.dsl.ExternalDocumentation
import io.heapy.komok.tech.api.dsl.Encoding
import io.heapy.komok.tech.api.dsl.ServerVariable
import io.heapy.komok.tech.api.dsl.OAuthFlows
import io.heapy.komok.tech.api.dsl.OAuthFlow

// Reference handling
import io.heapy.komok.tech.api.dsl.Referenceable
import io.heapy.komok.tech.api.dsl.Direct
import io.heapy.komok.tech.api.dsl.Reference
import io.heapy.komok.tech.api.dsl.ReferenceResolver

// Type aliases
import io.heapy.komok.tech.api.dsl.Paths        // Map<String, PathItem>
import io.heapy.komok.tech.api.dsl.Content       // Map<String, MediaType>
import io.heapy.komok.tech.api.dsl.Responses     // Map<String, Referenceable<Response>>
import io.heapy.komok.tech.api.dsl.Callback       // Map<String, PathItem>
import io.heapy.komok.tech.api.dsl.SecurityRequirement // Map<String, List<String>>

// Enums
import io.heapy.komok.tech.api.dsl.ParameterLocation
import io.heapy.komok.tech.api.dsl.ParameterStyle
import io.heapy.komok.tech.api.dsl.SecuritySchemeType
import io.heapy.komok.tech.api.dsl.ApiKeyLocation
import io.heapy.komok.tech.api.dsl.EncodingStyle

// DSL entry point
import io.heapy.komok.tech.api.dsl.openAPI

// Schema inference
import io.heapy.komok.tech.api.dsl.schemaOf
import io.heapy.komok.tech.api.dsl.SchemaInference
import io.heapy.komok.tech.api.dsl.SchemaInferenceConfig

// Schema annotations
import io.heapy.komok.tech.api.dsl.Min
import io.heapy.komok.tech.api.dsl.Max
import io.heapy.komok.tech.api.dsl.SchemaDescription
import io.heapy.komok.tech.api.dsl.SchemaTitle
import io.heapy.komok.tech.api.dsl.SchemaPattern
import io.heapy.komok.tech.api.dsl.Email
import io.heapy.komok.tech.api.dsl.Uuid
import io.heapy.komok.tech.api.dsl.NotBlank
import io.heapy.komok.tech.api.dsl.Positive
import io.heapy.komok.tech.api.dsl.UniqueItems
// ... and more (see SchemaAnnotations.kt)

// Specification extensions
import io.heapy.komok.tech.api.dsl.specificationExtensions
```

## Key Components

- `OpenAPI` - Root document object, validates version pattern `^3.2.\d+(-.+)?$` and requires at least one of paths/components/webhooks
- `openAPI { }` - Top-level DSL builder function that returns an `OpenAPI` instance
- `Referenceable<T>` - Sealed interface for polymorphic inline objects (`Direct<T>`) vs JSON Pointer references (`Reference`)
- `ReferenceResolver` - Resolves `#/components/<type>/<name>` references within an OpenAPI document, supports chaining with max depth 10
- `schemaOf<T>()` - Infers JSON Schema from Kotlin types using reflection, supports constraint annotations
- `ReferenceableSerializer<T>` - Custom Kotlinx Serialization serializer for `Referenceable<T>` polymorphism

## Usage Pattern

### Building an OpenAPI document with DSL

```kotlin
val api = openAPI {
    openapi = OpenAPI.VERSION_3_2_0
    info {
        title = "Pet Store API"
        version = "1.0.0"
        description = "A sample API for a pet store"
        contact {
            name = "API Support"
            email = "support@example.com"
        }
        license {
            name = "Apache 2.0"
            identifier = "Apache-2.0"
        }
    }
    servers {
        server {
            url = "https://api.example.com/v1"
            description = "Production"
        }
    }
    tags {
        tag {
            name = "pets"
            description = "Pet operations"
        }
    }
    paths {
        "/pets" to pathItem {
            get {
                operationId = "listPets"
                summary = "List all pets"
                tags = listOf("pets")
                parameters {
                    queryParameter("limit", required = false) {
                        type = "integer"
                        minimum = 1
                        maximum = 100
                    }
                }
                responses {
                    ok {
                        description = "A list of pets"
                        content {
                            json {
                                schema {
                                    type = "array"
                                    items {
                                        ref = "#/components/schemas/Pet"
                                    }
                                }
                            }
                        }
                    }
                    default {
                        description = "Unexpected error"
                    }
                }
            }
            post {
                operationId = "createPet"
                summary = "Create a pet"
                tags = listOf("pets")
                requestBody {
                    required = true
                    content {
                        json {
                            schema {
                                ref = "#/components/schemas/Pet"
                            }
                        }
                    }
                }
                responses {
                    created { description = "Pet created" }
                    badRequest { description = "Invalid input" }
                }
            }
        }
    }
    components {
        schemas {
            "Pet" to schema {
                type = "object"
                required = listOf("id", "name")
                properties {
                    "id" to schema { type = "integer"; format = "int64" }
                    "name" to schema { type = "string" }
                    "tag" to schema { type = "string" }
                }
            }
        }
        securitySchemes {
            "bearerAuth" to securityScheme {
                type = SecuritySchemeType.HTTP
                scheme = "bearer"
                bearerFormat = "JWT"
            }
        }
    }
    security {
        requirement("bearerAuth")
    }
}
```

### Building model objects directly (without DSL)

All model classes are `@Serializable` data classes that can be constructed directly:

```kotlin
val api = OpenAPI(
    openapi = "3.2.0",
    info = Info(
        title = "My API",
        version = "1.0.0",
    ),
    paths = mapOf(
        "/health" to PathItem(
            get = Operation(
                summary = "Health check",
                responses = mapOf(
                    "200" to Direct(Response(description = "OK")),
                ),
            ),
        ),
    ),
)
```

### Serialization

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

val json = Json { prettyPrint = true }

// Serialize to JSON
val jsonString = json.encodeToString(api)

// Deserialize from JSON
val parsed = json.decodeFromString<OpenAPI>(jsonString)
```

### Schema inference from Kotlin types

```kotlin
data class User(
    @SchemaDescription("Unique identifier")
    val id: Long,
    @NotBlank
    val name: String,
    @Email
    val email: String,
    @Positive
    val age: Int?,
)

val userSchema: Schema = schemaOf<User>()
```

### Using references

```kotlin
// Inline object
val direct: Referenceable<Response> = Direct(Response(description = "OK"))

// JSON Pointer reference
val ref: Referenceable<Response> = Reference(
    ref = "#/components/responses/NotFound",
    summary = "Not found response",
)

// Resolve references
val resolver = ReferenceResolver(api)
val resolved: Response = resolver.resolveResponse("#/components/responses/NotFound")
```

### Specification extensions

```kotlin
val api = OpenAPI(
    openapi = "3.2.0",
    info = Info(title = "My API", version = "1.0.0"),
    paths = mapOf(),
    extensions = specificationExtensions(
        "x-custom-field" to JsonPrimitive("value"),
    ),
)
```

## Architecture

### Model Layer
All OpenAPI 3.2 objects are `@Serializable` Kotlin data classes with init-block validation. The object hierarchy mirrors the OpenAPI specification: `OpenAPI` -> `Info`, `Server`, `PathItem` -> `Operation` -> `Parameter`, `RequestBody`, `Response` -> `MediaType`, `Header`, `Link`, etc.

### Reference Polymorphism
Components that can be referenced use `Referenceable<T>`:
- `Direct<T>` wraps an inline object
- `Reference` holds a `$ref` JSON Pointer string with optional `summary` and `description`
- Custom `ReferenceableSerializer<T>` handles transparent serialization (Direct unwraps, Reference serializes as `{"$ref": "..."}`)

### DSL Layer
Every complex object has a corresponding builder class (e.g., `OpenAPIBuilder`, `InfoBuilder`, `OperationBuilder`). Builders use inline functions for zero-overhead DSL nesting and enforce fail-fast validation in `build()`.

### Schema Inference
`SchemaInference` uses Kotlin reflection to generate JSON Schema from types. It handles primitives, collections, data classes, enums, sealed classes, and nullable types. Annotations like `@Min`, `@Max`, `@Email`, `@SchemaPattern` map to JSON Schema keywords.

## Source Files

### Model Classes
- `OpenAPI.kt` - Root document object
- `Info.kt` - API metadata (title, version, summary, description)
- `Contact.kt` - Contact information
- `License.kt` - License metadata (name, identifier or url)
- `Server.kt` - Server connectivity info
- `ServerVariable.kt` - URL template variable
- `PathItem.kt` - Path item with HTTP method operations, defines `Paths` typealias
- `Operation.kt` - Single operation, defines `Callback` typealias
- `Parameter.kt` - Parameter definition with location/style enums
- `RequestBody.kt` - Request payload
- `Response.kt` - Response definition, defines `Responses` typealias
- `Header.kt` - Response/request header
- `MediaType.kt` - Media type with schema/examples, defines `Content` typealias
- `Encoding.kt` - Encoding metadata for multipart
- `Schema.kt` - JSON Schema wrapper around `JsonElement`
- `Example.kt` - Example value
- `Link.kt` - Design-time link between responses
- `SecurityScheme.kt` - Security scheme definitions with OAuth flows
- `SecurityRequirement.kt` - Security requirement typealias
- `Tag.kt` - Operation tag with metadata
- `ExternalDocumentation.kt` - External documentation reference
- `Components.kt` - Reusable component registry
- `OpenAPIObject.kt` - Base interfaces (`OpenAPIObject`, `SupportsExtensions`, `Referenceable`, `Direct`, `Reference`)

### DSL Builders
- `ComponentsAndRootDsl.kt` - `OpenAPIBuilder`, `InfoBuilder`, `LinkBuilder`, `openAPI()` entry point
- `CoreValueObjectDsl.kt` - `ContactBuilder`, `LicenseBuilder`, `TagBuilder`, `ExternalDocumentationBuilder`
- `OperationAndPathDsl.kt` - Operation, path, parameter list, and security requirement builders
- `RequestAndResponseDsl.kt` - Request body, response, headers, and links builders
- `ServerDsl.kt` - Server and server variable builders
- `SecurityDsl.kt` - Security scheme and OAuth flow builders
- `SchemaAndExampleDsl.kt` - Schema, example, and content builders
- `ParameterAndHeaderDsl.kt` - Parameter builder with convenience functions (`pathParameter`, `queryParameter`, `headerParameter`, `cookieParameter`)

### Serialization
- `ReferenceableSerializer.kt` - Custom serializers for `Referenceable<T>` polymorphism (15 concrete serializers)

### Utilities
- `SchemaInference.kt` - Runtime schema inference from Kotlin types via reflection
- `SchemaAnnotations.kt` - 30+ annotations for declarative JSON Schema constraints
- `ReferenceResolver.kt` - `$ref` JSON Pointer resolution within OpenAPI components
- `SpecificationExtensions.kt` - Helpers for `x-*` custom properties
