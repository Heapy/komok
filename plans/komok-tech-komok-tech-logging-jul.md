# komok-tech/komok-tech-logging-jul Plan

## Scope

JUL bridge dependency exposure, message filtering, root handler installation, allocation cost, fallback behavior, timestamps, and tests.

## Severity-Ranked Issues

### High

- The module exposes runtime types in its public API while declaring the runtime dependency as `implementation`.

### Medium

- Raw JUL messages can be forwarded without applying expected filtering or normalization.
- Root handler installation can duplicate handlers and is not clearly reentrant.
- Pre-filter allocation adds avoidable overhead for disabled JUL records.
- Coverage is missing for filtering and bridge installation behavior.

### Low

- Fallback and timestamp behavior are under-covered.

## Fix Steps

1. Promote public runtime dependencies to `api` or hide runtime types from the public surface.
2. Apply filtering before building expensive bridged events.
3. Make root handler installation idempotent and thread-safe.
4. Add tests for duplicate installation, filtering, disabled records, fallback behavior, and timestamp mapping.

## Verification

- Run `./gradlew :komok-tech:komok-tech-logging-jul:test`.
- Include a test that installs the bridge twice and confirms only one handler is active.
