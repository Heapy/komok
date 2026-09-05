# komok-tech/komok-tech-to-be-injected Plan

## Scope

Injection helper module dependency exposure, mutable bean behavior, logging context API surface, error output, and tests.

## Severity-Ranked Issues

### High

- The module exposes `LoggingContext` from a public API while hiding the logging API dependency as `implementation`.

### Medium

- `MutableBean` concurrency and mutation behavior is not covered.

### Low

- Error messages and `toString` output can expose configured values.
- Mutation and `toString` behavior are under-tested.

## Fix Steps

1. Promote logging API dependency to `api` or remove `LoggingContext` from the public surface.
2. Add tests for `MutableBean` mutation order, repeated mutation, and concurrent access if the type is intended to be thread-safe.
3. Redact sensitive configured values in errors and diagnostic string output.
4. Add whole-object tests for mutation outcomes and string rendering.

## Verification

- Run `./gradlew :komok-tech:komok-tech-to-be-injected:test`.
- Compile a consumer fixture to confirm public dependency exposure is correct.
