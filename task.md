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

### Phase 15: Complete Referenceable Support & Reference Resolution

#### 15.1 Complete Referenceable Type Gaps
Already done: `Referenceable<T>` sealed interface, `Direct<T>`, `Reference`, `ReferenceableSerializer`, and support for Parameter, Header, Example, RequestBody, Link, Response, SecurityScheme.

Remaining work to match the OpenAPI 3.2 JSON Schema:
- [ ] Add `Referenceable<MediaType>` support: `Content` type alias becomes `Map<String, Referenceable<MediaType>>` (JSON Schema `content` uses `media-type-or-reference`)
- [ ] Add `ReferenceableMediaTypeSerializer` and `ReferenceableMediaTypeMapSerializer`
- [ ] Update `Components.mediaTypes` to `Map<String, Referenceable<MediaType>>`
- [ ] Add `Referenceable<Callback>` support: `Operation.callbacks` becomes `Map<String, Referenceable<Callback>>` (JSON Schema uses `callbacks-or-reference`)
- [ ] Add `ReferenceableCallbackSerializer` and `ReferenceableCallbackMapSerializer`
- [ ] Update `Components.callbacks` to `Map<String, Referenceable<Callback>>`
- [ ] Note: `Callback` is currently a type alias (`Map<String, PathItem>`) — needs to become a proper class implementing `OpenAPIObject` to work with `Referenceable<T>`
- [ ] Restore `Encoding.headers: Map<String, Referenceable<Header>>` (currently commented out)
- [ ] Restore `Response.links: Map<String, Referenceable<Link>>` (currently commented out as TODO)
- [ ] Add `Referenceable<RequestBody>` to `Operation.requestBody` (JSON Schema uses `request-body-or-reference`)
- [ ] Update DSL builders for new Referenceable fields (Content, Callbacks, Encoding headers, Response links, Operation requestBody)
- [ ] Test: MediaType reference serialization/deserialization round-trip
- [ ] Test: Callback reference serialization/deserialization round-trip
- [ ] Test: Restored Encoding.headers and Response.links round-trip
- [ ] Test: Operation.requestBody as reference round-trip

#### 15.2 Reference Resolution
Resolve `$ref` pointers to actual objects within a document:
- [ ] Create `ReferenceResolver` that takes an `OpenAPI` document
- [ ] Implement JSON Pointer parsing (`#/components/schemas/Pet` → path segments)
- [ ] Implement resolution for all component types (schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks, pathItems, mediaTypes)
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

### Phase 16: Documentation and Examples
- [ ] Create llms.txt for komok-tech-api-dsl and komok-tech-api-dsl-ui modules

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

