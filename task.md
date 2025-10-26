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

### Phase 1: Project Setup and Foundation
- [x] Module setup with Kotlin Serialization, JUnit, and json-schema-validator
- [x] Create base interfaces/annotations for OpenAPI objects
- [x] Set up test infrastructure with JSON schema validation helpers
- [x] Create utility functions for specification extensions (x- properties)

### Phase 2: Core Value Objects
These are simple objects without complex dependencies:

#### 2.1 Basic Information Objects
- [x] Contact object (name, url, email)
- [x] License object (name, identifier, url)
- [x] ExternalDocumentation object (description, url)
- [x] Tag object (name, summary, description, externalDocs, parent, kind)
- [x] Reference object ($ref, summary, description)
- [x] Test: Basic information objects with JSON schema validation

#### 2.2 Server Objects
- [x] ServerVariable object (enum, default, description)
- [x] Server object (url, description, name, variables)
- [x] Test: Server objects with JSON schema validation

### Phase 3: Schema and Data Modeling
- [x] Schema object (integrate with JSON Schema - can be object or boolean)
- [x] Example object (summary, description, dataValue, serializedValue, value, externalValue)
- [x] Test: Schema and Example objects with JSON schema validation

### Phase 4: Parameter and Header Modeling
- [x] Parameter object (name, in, description, required, deprecated, schema, content, style, explode, allowReserved, allowEmptyValue)
- [x] Header object (description, required, deprecated, schema, content, style, explode)
- [x] Test: Parameter location types (query, querystring, header, path, cookie)
- [x] Test: Parameter styles (matrix, label, simple, form, spaceDelimited, pipeDelimited, deepObject, cookie)
- [x] Test: Header objects with JSON schema validation

### Phase 5: Content and Media Types
- [x] Encoding object (contentType, style, explode, allowReserved, encoding, prefixEncoding, itemEncoding)
- [x] MediaType object (description, schema, itemSchema, encoding, prefixEncoding, itemEncoding, example, examples)
- [x] Header object (description, required, deprecated, schema, content, style, explode, example, examples)
- [x] Test: Content objects (map of MediaType)
- [x] Test: Encoding configurations with mutual exclusivity validation
- [ ] TODO: Add Referenceable support for MediaType, Example, and Header (deferred to later phase)

### Phase 6: Request and Response Modeling
- [x] RequestBody object (description, content, required)
- [x] Response object (summary, description, headers, content, links)
- [x] Responses container object (default, status code patterns)
- [x] Test: Request/Response bodies with various content types
- [x] Test: Response status code patterns (1XX, 2XX, 3XX, 4XX, 5XX)
- [ ] TODO: Add Link support (deferred to later phase)

### Phase 7: Operations and Paths
- [x] Operation object (tags, summary, description, externalDocs, operationId, parameters, requestBody, responses, callbacks, deprecated, servers)
- [x] PathItem object ($ref, summary, description, servers, parameters, additionalOperations, get, put, post, delete, options, head, patch, trace, query)
- [x] Paths container object (path pattern validation)
- [x] Callback object (typealias to Map<String, PathItem>)
- [x] Test: All HTTP methods (GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE, QUERY)
- [x] Test: Path patterns and parameters
- [x] Test: Callback definitions

### Phase 8: Security Modeling
- [x] OAuthFlows objects (implicit, password, clientCredentials, authorizationCode, deviceAuthorization)
- [x] SecurityScheme object (type: apiKey, http, mutualTLS, oauth2, openIdConnect)
- [x] SecurityRequirement object
- [x] Test: All security scheme types
- [x] Test: OAuth 2.0 flows
- [x] Test: OpenID Connect configuration
- [x] Updated Operation object to include security field

### Phase 9: Components
- [x] Components object (schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks, pathItems, mediaTypes)
- [x] Link object (operationRef, operationId, parameters, requestBody, description, server)
- [x] Test: All component types (schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks, pathItems, mediaTypes)
- [x] Test: Component naming patterns (^[a-zA-Z0-9._-]+$)
- [x] Test: Link validation (operationRef XOR operationId)
- [ ] TODO: Component reference resolution (deferred - requires full OpenAPI document context)

### Phase 10: Root OpenAPI Document
- [x] Info object (title, summary, description, termsOfService, contact, license, version)
- [x] OpenAPI root object (openapi, $self, info, jsonSchemaDialect, servers, paths, webhooks, components, security, tags, externalDocs)
- [x] Test: OpenAPI version pattern (^3\.2\.\d+(-.+)?$)
- [x] Test: Document validation (must have paths, components, or webhooks)
- [x] Test: $self validation (must not contain fragment)
- [x] Test: All OpenAPI properties (servers, security, tags, externalDocs, webhooks)
- [x] Test: Real-world examples (Petstore, Stripe, GitHub)

### Phase 11: Kotlin DSL Builders
For each model class, create idiomatic Kotlin DSL:

- [ ] Info DSL with contact and license builders
- [ ] Server DSL with variables builder
- [ ] Parameter DSL with style and schema builders
- [ ] MediaType DSL with encoding and examples builders
- [ ] RequestBody DSL with content builder
- [ ] Response DSL with headers and content builders
- [ ] Operation DSL with parameters, requestBody, and responses builders
- [ ] PathItem DSL with operation builders for each HTTP method
- [ ] SecurityScheme DSL with OAuth flows builder
- [ ] Components DSL with typed component builders
- [ ] OpenAPI root DSL with fluent API
- [ ] Test: DSL usage examples for each builder

### Phase 12: API Documentation UI (komok-tech-api-dsl-ui module)
Create a web-server-agnostic UI module for displaying OpenAPI documentation:

#### 12.1 Module Setup
- [ ] Create `:komok-tech:komok-tech-api-dsl-ui` module
- [ ] Add kotlinx-html dependency for HTML generation
- [ ] Set up build configuration for embedding static resources (CSS, JS)
- [ ] Create main function signature: `fun renderOpenApiDoc(openapi: OpenAPI): String`

#### 12.2 HTML Structure and Layout
- [ ] Design semantic HTML structure for OpenAPI documentation
- [ ] Implement header section (API title, version, description)
- [ ] Implement server information display
- [ ] Implement navigation/table of contents for endpoints
- [ ] Implement main content area for operation details
- [ ] Implement sidebar for model schemas
- [ ] Support for external documentation links
- [ ] Support for security scheme documentation

#### 12.3 Styling (Embedded CSS)
- [ ] Create clean, modern CSS layout (embedded in HTML head)
- [ ] Implement responsive design for mobile/tablet/desktop
- [ ] Design HTTP method badges (GET, POST, PUT, DELETE, etc.)
- [ ] Style request/response sections
- [ ] Style schema tables and object hierarchies
- [ ] Implement dark/light theme support
- [ ] Add smooth transitions and micro-interactions
- [ ] Ensure readability and accessibility (WCAG compliant)

#### 12.4 Interactive JavaScript UI (Preact + ESM)
- [ ] Set up Preact-based component architecture
- [ ] Use ES modules (mjs) for modern JavaScript delivery
- [ ] Implement collapsible sections for operations
- [ ] Create interactive schema browser with expand/collapse
- [ ] Add request/response example display with syntax highlighting
- [ ] Implement "Try it out" functionality (optional, lightweight)
- [ ] Create search/filter functionality for endpoints
- [ ] Add deep linking support (URL fragments for specific operations)
- [ ] Implement keyboard navigation support

#### 12.5 Performance Optimization
- [ ] Minimize JavaScript bundle size (target: <50KB total)
- [ ] Implement virtual scrolling for APIs with many endpoints
- [ ] Lazy load sections on demand (intersection observer)
- [ ] Optimize initial render time (target: <100ms for HTML generation)
- [ ] Use CSS containment for layout performance
- [ ] Minimize DOM nodes for large schemas
- [ ] Add performance monitoring hooks
- [ ] Benchmark against Swagger UI for comparison

#### 12.6 Testing
- [ ] Test: HTML generation from minimal OpenAPI object
- [ ] Test: HTML generation from complex OpenAPI documents
- [ ] Test: Validate HTML structure and semantics
- [ ] Test: Verify all OpenAPI elements are rendered
- [ ] Test: CSS is properly embedded
- [ ] Test: JavaScript is properly embedded
- [ ] Test: Generated HTML size is reasonable
- [ ] Test: Performance benchmarks for various document sizes
- [ ] Test: Accessibility compliance (ARIA labels, semantic HTML)

#### 12.7 Documentation and Examples
- [ ] Add KDoc for public API
- [ ] Create usage examples for different web servers (Ktor, Spring, etc.)
- [ ] Document customization options (themes, layouts)
- [ ] Add example showcasing generated UI

### Phase 13: Validation and Integration
- [ ] Add model-level validation (required fields, pattern matching, mutual exclusivity)
- [ ] Implement specification extensions support (x- properties)
- [ ] Add reference resolution utilities
- [ ] Test: Complex nested documents
- [ ] Test: Real-world OpenAPI examples (Stripe, GitHub, Petstore)
- [ ] Test: Ensure 100% code coverage for all model classes

### Phase 14: Documentation and Examples
- [ ] Add KDoc documentation to all public APIs
- [ ] Create example OpenAPI documents using the DSL
- [ ] Add validation error messages with helpful hints
- [ ] Create user guide for the DSL

### Phase 15: Performance and Optimization
- [ ] Benchmark serialization/deserialization performance
- [ ] Optimize memory usage for large documents
- [ ] Add lazy initialization where appropriate

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

