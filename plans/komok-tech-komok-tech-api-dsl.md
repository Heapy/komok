# komok-tech/komok-tech-api-dsl Plan

## Scope

OpenAPI DSL serialization, schema inference, pointer escaping, reflection performance, and coverage gaps.

## Severity-Ranked Issues

### High

- Extension fields are serialized under literal `extensions` instead of OpenAPI-compatible `x-*` keys.

### Medium

- `SchemaInference` branches are under-covered, leaving enum, collection, nullable, and nested-object behavior at risk.

### Low

- JSON pointer unescaping behavior is not covered.
- Reflection-heavy schema inference appears to use repeated scans with avoidable `O(n^2)` behavior on larger model graphs.

## Fix Steps

1. Change extension serialization to emit top-level `x-*` keys and add regression tests.
2. Backfill `SchemaInference` tests for primitive, enum, collection, map, nullable, recursive, and nested object cases.
3. Add JSON pointer escaping and unescaping tests.
4. Cache reflection metadata or restructure inference traversal to avoid repeated full scans.

## Verification

- Run `./gradlew :komok-tech:komok-tech-api-dsl:test`.
- Compare generated OpenAPI output against expected documents containing `x-*` extensions and representative schemas.
