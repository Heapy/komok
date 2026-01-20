I want to develop Komok in the API-first approach.
For that, I want to author Kotlin DSL to express OpenAPI 3.2 documents.
And to express OpenAPI 3.2 documents, I need to create Kotlin classes that would model OpenAPI 3.2 documents.
I'd like to use Kotlinx Serialization to do that.

To learn about OpenAPI 3.2 use @komok-tech/komok-tech-api-dsl/src/test/resources/doc.md and @komok-tech/komok-tech-api-dsl/src/test/resources/openapi-v3.2.0-json-schema.json

Put code in :komok-tech:komok-tech-api-dsl module.
The UI rendering code will go in :komok-tech:komok-tech-api-dsl-ui module (Phase 12).

Let's first create OpenAPI 3.2 document model classes and write tests that would generate JSON files from them.
Use com.networknt:json-schema-validator to validate generated JSON files against JSON Schema.

Implement at least 100% coverage of model classes.

Remember that each test should be self-contained and independent. Put all initialization code in test itself. Reusing objects and methods is fine, just make sure that test calling initialization logic.

Update this document with comprehensive list of tasks.

---

## Comprehensive Task Breakdown

### Phase 11: Kotlin DSL Builders
Create idiomatic Kotlin DSL for each model class. DSL builders should use **fail-fast validation** - throw meaningful exceptions immediately when invalid values are provided (e.g., missing required fields, invalid patterns, mutual exclusivity violations).

#### 11.1 Core Value Object DSLs
- [x] Contact DSL builder
- [x] License DSL builder
- [x] ExternalDocumentation DSL builder
- [x] Tag DSL builder
- [x] Test: Core value object DSL examples

#### 11.2 Server DSLs
- [x] ServerVariable DSL builder
- [x] Server DSL with variables builder
- [x] Test: Server DSL examples

#### 11.3 Schema and Example DSLs
- [x] Schema DSL builder (JSON Schema integration)
- [x] Example DSL builder
- [x] Test: Schema and Example DSL examples

#### 11.3.1 Schema Inference from Kotlin Types
- [x] Add kotlin-reflect dependency
- [x] Create constraint annotations (@Min, @Max, @MinLength, @MaxLength, @NotEmpty, @NotBlank, @Pattern, @Positive, @Negative, etc.)
- [x] Create format annotations (@Email, @Url, @Uuid, @Date, @DateTime, @Format)
- [x] Create collection annotations (@MinItems, @MaxItems, @UniqueItems)
- [x] Create documentation annotations (@SchemaTitle, @SchemaDescription)
- [x] Create SchemaInference engine with support for:
  - Primitive types (String, Int, Long, Float, Double, Boolean)
  - Nullable types (generates `nullable: true`)
  - Collections (List, Set -> array schema)
  - Maps (-> additionalProperties)
  - Enums (-> enum constraint)
  - Sealed classes/interfaces (-> oneOf)
  - Nested data classes
  - Recursive type handling
  - @SerialName support
- [x] Create schemaOf<T>() inline function
- [x] Test: Schema inference examples (50+ test cases)

#### 11.4 Parameter and Header DSLs
- [x] Parameter DSL with style and schema builders
- [x] Header DSL builder
- [x] Test: Parameter and Header DSL examples

#### 11.5 Content and Encoding DSLs
- [ ] Encoding DSL builder
- [x] MediaType DSL with encoding and examples builders
- [ ] Test: Content and Encoding DSL examples

#### 11.6 Request and Response DSLs
- [ ] RequestBody DSL with content builder
- [ ] Response DSL with headers and content builders
- [ ] Responses container DSL
- [ ] Test: Request and Response DSL examples

#### 11.7 Operation and Path DSLs
- [ ] Operation DSL with parameters, requestBody, and responses builders
- [ ] PathItem DSL with operation builders for each HTTP method (get, post, put, delete, etc.)
- [ ] Paths container DSL
- [ ] Callback DSL
- [ ] Test: Operation and Path DSL examples

#### 11.8 Security DSLs
- [ ] OAuthFlow DSL builders (implicit, password, clientCredentials, authorizationCode, deviceAuthorization)
- [ ] SecurityScheme DSL for all types (apiKey, http, mutualTLS, oauth2, openIdConnect)
- [ ] SecurityRequirement DSL
- [ ] Test: Security DSL examples

#### 11.9 Components and Root DSLs
- [ ] Components DSL with typed component builders
- [ ] Link DSL builder
- [ ] Info DSL with contact and license builders
- [ ] OpenAPI root DSL with fluent API
- [ ] Test: Components and Root DSL examples

### Phase 12: API Documentation UI (komok-tech-api-dsl-ui module)
Create a web-server-agnostic UI module for displaying OpenAPI documentation:

#### 12.1 Module Setup
- [x] Create `:komok-tech:komok-tech-api-dsl-ui` module
- [x] Add kotlinx-html dependency for HTML generation
- [x] Set up build configuration for embedding static resources (CSS, JS)
- [x] Create main function signature: `fun renderOpenApiDoc(openapi: OpenAPI): String`

#### 12.2 HTML Structure and Layout
- [x] Design semantic HTML structure for OpenAPI documentation
- [x] Implement header section (API title, version, description)
- [x] Implement server information display
- [x] Implement navigation/table of contents for endpoints
- [x] Implement main content area for operation details
- [x] Implement sidebar for model schemas
- [ ] Support for external documentation links (not fully implemented for tags and info)
- [ ] Support for security scheme documentation (not rendered yet)

#### 12.3 Styling (Embedded CSS)
- [x] Create clean, modern CSS layout (embedded in HTML head)
- [x] Implement responsive design for mobile/tablet/desktop
- [x] Design HTTP method badges (GET, POST, PUT, DELETE, etc.)
- [x] Style request/response sections
- [x] Style schema tables and object hierarchies
- [x] Implement dark/light theme support
- [x] Add smooth transitions and micro-interactions
- [x] Ensure readability and accessibility (WCAG compliant)

#### 12.4 Interactive JavaScript UI (Vanilla JS - simplified approach)
Note: Implementation uses vanilla JavaScript instead of Preact for simplicity
- [x] Theme toggle functionality with localStorage persistence
- [x] Create search/filter functionality for endpoints
- [x] Add deep linking support (URL fragments for specific operations)
- [x] Implement keyboard navigation support (Ctrl/Cmd+K for search, Ctrl/Cmd+D for theme)
- [x] Smooth scrolling for navigation
- [x] Active section highlighting with IntersectionObserver
- [ ] Implement collapsible sections for operations (not implemented)
- [ ] Create interactive schema browser with expand/collapse (not implemented)
- [ ] Add request/response example display with syntax highlighting (not implemented)

#### 12.8 HTTP File Generation
Generate downloadable `.http` files for use with IntelliJ IDEA / VS Code REST Client:
- [ ] Create `fun generateHttpFile(openapi: OpenAPI): String` function
- [ ] Generate one request per operation with documentation comments
- [ ] Include path parameters, query parameters, headers, and request body examples
- [ ] Support for multiple servers (base URLs)
- [ ] Add download button in UI to get the .http file
- [ ] Test: Generated .http file syntax is valid

#### 12.5 Performance Optimization
- [x] Add performance monitoring hooks (console logging)
- [x] Use IntersectionObserver for active section tracking
- [x] Minimal JavaScript bundle (vanilla JS, no frameworks)
- [ ] Implement virtual scrolling for APIs with many endpoints
- [ ] Optimize initial render time (target: <100ms for HTML generation) - needs benchmarking
- [ ] Use CSS containment for layout performance
- [ ] Minimize DOM nodes for large schemas
- [ ] Benchmark against Swagger UI for comparison

#### 12.6 Testing
- [x] Test: HTML generation from minimal OpenAPI object
- [x] Test: Validate HTML structure and semantics
- [x] Test: Verify OpenAPI elements are rendered (info, servers, endpoints, operations, parameters)
- [x] Test: CSS is properly embedded
- [x] Test: JavaScript is properly embedded
- [x] Test: Generated HTML size is reasonable
- [ ] Test: HTML generation from complex OpenAPI documents (needs more comprehensive tests)
- [ ] Test: Performance benchmarks for various document sizes
- [ ] Test: Accessibility compliance (ARIA labels, semantic HTML) - needs specific test

#### 12.7 Documentation and Examples
- [x] Add KDoc for public API (partial - main functions documented)
- [x] Manual test example with Petstore (PetstoreManualTest.kt)
- [ ] Create usage examples for different web servers (Ktor, Spring, etc.)
- [ ] Document customization options (themes, layouts)
- [ ] Add comprehensive example showcasing all UI features

### Phase 13: Validation and Integration
- [ ] Add model-level validation (required fields, pattern matching, mutual exclusivity)
- [ ] Test: Complex nested documents
- [ ] Test: Real-world OpenAPI 3.2 examples (Stripe, GitHub APIs - need to obtain/convert to 3.2)
- [ ] Test: Ensure 100% code coverage for all model classes

### Phase 14: Documentation and Examples
- [ ] Add KDoc documentation to all public APIs
- [ ] Create example OpenAPI documents using the DSL
- [ ] Add validation error messages with helpful hints
- [ ] Create user guide for the DSL

### Phase 15: Reference Resolution & Referenceable Types
Advanced reference handling for complete OpenAPI document processing.

#### 15.1 Referenceable Type Support
Extend model classes to accept either inline definitions or `$ref` references:
- [ ] Create `Referenceable<T>` sealed type (either `Inline<T>` or `Ref`)
- [ ] Update MediaType fields to support Referenceable where spec allows
- [ ] Update Example fields to support Referenceable where spec allows
- [ ] Update Header fields to support Referenceable where spec allows
- [ ] Custom serializer for Referenceable that outputs either inline object or `{"$ref": "..."}`
- [ ] Test: Referenceable serialization/deserialization round-trip

#### 15.2 Reference Resolution
Resolve `$ref` pointers to actual objects within a document:
- [ ] Create `ReferenceResolver` that takes an `OpenAPI` document
- [ ] Implement JSON Pointer parsing (`#/components/schemas/Pet` → path segments)
- [ ] Implement resolution for all component types (schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks, pathItems)
- [ ] Return resolved object or error if reference is invalid
- [ ] Test: Resolution of valid references
- [ ] Test: Error handling for missing references

#### 15.3 Reference Validation
Validate all references in a document:
- [ ] Detect references pointing to non-existent components
- [ ] Detect circular references (A → B → A)
- [ ] Report all validation errors with paths to problematic references
- [ ] Test: Circular reference detection
- [ ] Test: Missing reference detection
- [ ] Test: Valid document passes validation

---

## Implementation Notes

1. **Testing Strategy**: Each phase should include comprehensive tests that:
   - Serialize Kotlin objects to JSON
   - Validate JSON against the OpenAPI 3.2 JSON Schema
   - Deserialize JSON back to Kotlin objects
   - Verify round-trip serialization (object -> JSON -> object)

2. **Coverage Requirements**: Minimum 100% line coverage for model classes

3. **JSON Schema Validation**: Use `com.networknt:json-schema-validator` to validate all generated JSON

4. **Kotlinx Serialization**: Use `@Serializable` annotation with proper naming strategies and defaults

5. **Dependencies**: All test resources and validation schemas are in `src/test/resources/`

