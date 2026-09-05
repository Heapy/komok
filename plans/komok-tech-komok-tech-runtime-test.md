# komok-tech/komok-tech-runtime-test

## Scope

Integration harness for runtime/KSP generated graph behavior. Generated-code gaps are tracked under `komok-tech-runtime-ksp`.

## Severity-Ranked Issues

### Low

- Replacement tests assert that value/mock/spy replacements strip decorators and produce no log events. That normalizes wrapper bypass behavior instead of making lifecycle/logging semantics explicit.
- Several service tests call `graph.stop()` without first calling `graph.start()`, which can hide a stop-before-start lifecycle bug.

## Fix Steps

1. Revisit replacement tests and decide whether replacement bindings should preserve decorators, emit lifecycle/log events, or explicitly document bypass behavior.
2. Add focused assertions for the chosen replacement behavior.
3. Update service lifecycle tests to call `graph.start()` before `graph.stop()` where normal lifecycle behavior is intended.
4. Add a dedicated test for `graph.stop()` before `graph.start()` so that edge case is explicit.

## Verification

- Run `./gradlew :komok-tech:komok-tech-runtime-test:test`.
- If lifecycle behavior is shared with generated runtime wiring, also run the relevant `runtime-ksp` tests.
