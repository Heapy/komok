# komok-tech/komok-tech-runtime-wrappers Plan

## Scope

Runtime wrapper helpers for logging, metrics, and retry behavior, with attention to data exposure, allocation cost, failure behavior, and missing tests.

## Severity-Ranked Issues

### High

- `@Log(includeResult = true)` can log sensitive return values by default when wrappers are applied broadly.

### Medium

- `metricCall` performs timer lookup on every invocation, adding overhead to hot paths.
- Metric and logging wrapper failure paths are not covered by tests.

### Low

- `logCall` duration behavior when logging is disabled is untested.
- Retry delay/backoff behavior is not covered.

## Fix Steps

1. Make result logging opt-in per use case, add redaction support, and document safe defaults.
2. Cache metric timer lookups where wrapper configuration is stable.
3. Add tests for metric wrapper failures, logging wrapper failures, and disabled logging paths.
4. Add retry tests covering delay, backoff, exhaustion, and exception propagation.

## Verification

- Run `./gradlew :komok-tech:komok-tech-runtime-wrappers:test`.
- Confirm tests prove sensitive results are not logged unless explicitly enabled and that wrapper failures do not hide the original call semantics.
