# komok-tech/komok-tech-logging-slf4j Plan

## Scope

SLF4J provider initialization, enabled-level behavior, provider contract compliance, caller name handling, and coverage.

## Severity-Ranked Issues

### High

- Provider initialization with null or missing context can silently drop events.
- `isEnabled` returns based on `context != null`, bypassing configured filtering semantics.

### Medium

- Provider contract and ServiceLoader behavior are not covered enough to catch integration regressions.

### Low

- Enabled flags and caller name mapping lack focused tests.

## Fix Steps

1. Fail fast or provide an explicit fallback when provider context is missing instead of silently dropping events.
2. Make `isEnabled` delegate to the configured logging filter.
3. Add provider contract tests using SLF4J entry points and ServiceLoader discovery.
4. Add tests for enabled flags, marker handling if supported, caller names, and null-context behavior.

## Verification

- Run `./gradlew :komok-tech:komok-tech-logging-slf4j:test`.
- Add an integration test that logs through SLF4J and asserts filtering and event delivery.
