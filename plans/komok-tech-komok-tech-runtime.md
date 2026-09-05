# komok-tech/komok-tech-runtime Plan

## Scope

Lifecycle startup/shutdown ordering, Realm close semantics, provider lookup performance, graph fork efficiency, cleanup failure handling, and missing test coverage around rollback and JSON escaping.

## Severity-Ranked Issues

### High

- `Lifecycle.shutdown` can run before successful startup and realize shutdown providers on stop-before-start.

### Medium

- Shutdown hooks fail fast, so one failing hook can skip later cleanup.
- `Realm.close` cancels without joining, allowing provider bodies to continue after close returns.
- Provider reads do linear `callerStack` checks and allocate `callerStack + id` before memoized hits.
- `fork` snapshots edges and rebuilds reverse adjacency in `O(E)`.
- `closeOwned` swallows close failures without tests proving cleanup continues.
- Startup rollback failure suppression is uncovered by tests.

### Low

- `GraphPayload` JSON escaping branches are uncovered.

## Fix Steps

1. Guard lifecycle shutdown so it only runs after successful startup, and add stop-before-start coverage.
2. Change shutdown hook execution to collect failures while continuing later cleanup; suppress or aggregate failures deterministically.
3. Make `Realm.close` wait for cancelled provider work to finish before returning.
4. Fast-path memoized provider reads before caller-stack allocation, and replace linear recursion checks with cheaper tracking.
5. Optimize `fork` reverse-adjacency handling to avoid full edge rebuilds where possible.
6. Add focused tests for `closeOwned` cleanup continuation, startup rollback suppression, and `GraphPayload` escaping.

## Verification

- Run `./gradlew :komok-tech:komok-tech-runtime:test`.
- Confirm tests cover stop-before-start shutdown, failing shutdown hooks, close cancellation/join behavior, cleanup continuation, rollback suppression, memoized provider fast path, fork adjacency behavior, and JSON escaping branches.
