I want to develop Komok in the API-first approach.
For that, I want to author Kotlin DSL to express OpenAPI 3.2 documents.
And to express OpenAPI 3.2 documents, I need to create Kotlin classes that would model OpenAPI 3.2 documents.
I'd like to use Kotlinx Serialization to do that.

To learn about OpenAPI 3.2 use @komok-tech/komok-tech-api-dsl/src/test/resources/doc.md and @komok-tech/komok-tech-api-dsl/src/test/resources/openapi-v3.2.0-json-schema.json

Put code in :komok-tech:komok-tech-api-dsl module.

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
- [ ] RequestBody object (description, content, required)
- [ ] Response object (summary, description, headers, content, links)
- [ ] Responses container object (default, status code patterns)
- [ ] Test: Request/Response bodies with various content types
- [ ] Test: Response status code patterns (1XX, 2XX, 3XX, 4XX, 5XX)

### Phase 7: Operations and Paths
- [ ] Operation object (tags, summary, description, externalDocs, operationId, parameters, requestBody, responses, callbacks, deprecated, security, servers)
- [ ] PathItem object ($ref, summary, description, servers, parameters, additionalOperations, get, put, post, delete, options, head, patch, trace, query)
- [ ] Paths container object (path pattern validation)
- [ ] Callback object
- [ ] Test: All HTTP methods
- [ ] Test: Path patterns and parameters
- [ ] Test: Callback definitions

### Phase 8: Security Modeling
- [ ] OAuthFlows objects (implicit, password, clientCredentials, authorizationCode, deviceAuthorization)
- [ ] SecurityScheme object (type: apiKey, http, mutualTLS, oauth2, openIdConnect)
- [ ] SecurityRequirement object
- [ ] Test: All security scheme types
- [ ] Test: OAuth 2.0 flows
- [ ] Test: OpenID Connect configuration

### Phase 9: Components
- [ ] Components object (schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks, pathItems, mediaTypes)
- [ ] Link object (operationRef, operationId, parameters, requestBody, description, server)
- [ ] Test: Component reference resolution
- [ ] Test: All component types
- [ ] Test: Component naming patterns (^[a-zA-Z0-9._-]+$)

### Phase 10: Root OpenAPI Document
- [ ] Info object (title, summary, description, termsOfService, contact, license, version)
- [ ] OpenAPI root object (openapi, $self, info, jsonSchemaDialect, servers, paths, webhooks, components, security, tags, externalDocs)
- [ ] Test: OpenAPI version pattern (^3\.2\.\d+(-.+)?$)
- [ ] Test: Document validation (must have paths, components, or webhooks)
- [ ] Test: Default values (servers default to [{url: "/"}], jsonSchemaDialect default)

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

### Phase 12: Validation and Integration
- [ ] Add model-level validation (required fields, pattern matching, mutual exclusivity)
- [ ] Implement specification extensions support (x- properties)
- [ ] Add reference resolution utilities
- [ ] Test: Complex nested documents
- [ ] Test: Real-world OpenAPI examples (Stripe, GitHub, Petstore)
- [ ] Test: Ensure 100% code coverage for all model classes

### Phase 13: Documentation and Examples
- [ ] Add KDoc documentation to all public APIs
- [ ] Create example OpenAPI documents using the DSL
- [ ] Add validation error messages with helpful hints
- [ ] Create user guide for the DSL

### Phase 14: Performance and Optimization
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

