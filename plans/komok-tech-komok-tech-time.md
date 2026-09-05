# komok-tech/komok-tech-time Plan

## Scope

Time source abstractions, default zone behavior, test clock behavior, runtime graph wiring, and missing coverage.

## Severity-Ranked Issues

### Medium

- `DefaultTimeSource` behavior is not covered by tests.
- Runtime graph wiring for time components is not covered.

### Low

- `TestTimeSource` mutation and read behavior are not covered.
- System default zone lookup behavior can make tests or consumers sensitive to process-global timezone changes.

## Fix Steps

1. Add deterministic tests for `DefaultTimeSource` using controlled clock assumptions where possible.
2. Add runtime graph tests that prove the time source binding is available and has the expected type.
3. Add mutation/read tests for `TestTimeSource`.
4. Consider injecting or capturing zone configuration explicitly where consumers need stable behavior.

## Verification

- Run `./gradlew :komok-tech:komok-tech-time:test`.
- Include timezone-sensitive test cases that do not depend on the developer machine default timezone.
