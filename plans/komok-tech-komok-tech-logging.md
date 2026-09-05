# komok-tech/komok-tech-logging Plan

## Scope

Async logging handler behavior, file appender locking and flushing, MDC redaction, directory creation, JSON encoding, rolling behavior, and coverage.

## Severity-Ranked Issues

### High

- `AsyncHandler` uses `runBlocking`, which can block caller threads and defeat asynchronous logging under load.
- File appender locking and flush behavior can serialize hot paths and create latency spikes.

### Medium

- MDC redaction behavior is not clearly enforced.
- Directory creation may fail for paths with no parent.
- Async handler and rolling file behavior need focused tests.

### Low

- `JsonEncoder` stacktrace and key-collision behavior is under-covered.

## Fix Steps

1. Replace `runBlocking` on logging hot paths with non-blocking enqueue semantics and explicit backpressure policy.
2. Review file appender synchronization and flushing strategy; buffer safely without flushing every event unless configured.
3. Add explicit MDC redaction rules and tests.
4. Guard directory creation for parentless paths.
5. Add async handler tests for enqueue, overflow, shutdown, and exception handling.
6. Add rolling appender tests for size/date boundaries and file retention.
7. Add JSON encoder tests for stacktraces, nested exceptions, duplicate keys, and special characters.

## Verification

- Run `./gradlew :komok-tech:komok-tech-logging:test`.
- Add a small stress-style test or benchmark around async logging enqueue behavior.
