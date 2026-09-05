# komok-tech/komok-tech-logging-api Plan

## Scope

Logging API filtering, context timestamp behavior, lazy event fields, throwable serialization, MDC exposure, and tests.

## Severity-Ranked Issues

### High

- `Logger.log` performs eager event work before checking filters, so disabled log levels still pay allocation and supplier costs.
- `LoggingContext.now` is ignored, making timestamp behavior difficult to control and test.

### Medium

- Thread name capture is eager and should be deferred until an event is accepted.
- `ThrowableProxy` cycle and repeated-cause behavior is not covered.

### Low

- Raw MDC values can appear in `toString` output.
- Logger name behavior lacks focused tests.

## Fix Steps

1. Move filter checks before expensive event creation and supplier evaluation.
2. Use `LoggingContext.now` consistently for event timestamps.
3. Lazily capture thread name only for accepted events.
4. Add cycle-safe throwable serialization tests.
5. Redact or avoid raw MDC in diagnostic string output.
6. Add tests for logger names, disabled levels, timestamp source, MDC, and throwable edge cases.

## Verification

- Run `./gradlew :komok-tech:komok-tech-logging-api:test`.
- Include tests proving disabled logs do not evaluate message/result suppliers.
