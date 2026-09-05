# komok-tech/komok-tech-api-dsl-ui Plan

## Scope

API documentation UI generation, recursive schema rendering, link safety, embedded HTTP examples, serialization cost, and coverage gaps.

## Severity-Ranked Issues

### High

- Recursive references have no visited-set guard and can recurse indefinitely during rendering.

### Medium

- External links are rendered without clear URL scheme restrictions or `rel` hardening.
- Schema pretty-printing is repeated during rendering, adding avoidable CPU cost.
- `.http` examples are embedded eagerly, increasing output size and generation cost.
- Markdown and HTTP file rendering behavior lacks focused tests.

### Low

- Raw `.http` fields are not guarded against CRLF or formatting injection.
- Reference rendering branches are under-covered.

## Fix Steps

1. Add a visited-set guard for recursive schema references and render cycles deterministically.
2. Restrict or sanitize unsafe URL schemes and add `rel="noopener noreferrer"` where links open external targets.
3. Cache pretty-printed schemas per schema node or precompute them once per render.
4. Lazy-load or collapse large `.http` examples where practical.
5. Add tests for Markdown rendering, HTTP file output, recursive refs, unsafe URLs, CRLF handling, and reference rendering.

## Verification

- Run `./gradlew :komok-tech:komok-tech-api-dsl-ui:test`.
- Add a recursive schema fixture and confirm rendering terminates with stable output.
