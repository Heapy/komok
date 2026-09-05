# komok-server-core Remediation Plan

## Scope

Network/runtime behavior in `komok-server-core`: HTTP server lifecycle, Netty threading, binding, timeouts, shutdown, client resource use, WebSocket/predicate behavior, and request/response logging.

## Severity-Ranked Issues

### High

- `HttpRequestHandler` runs root handler coroutines on the Netty event loop, so blocking or slow application work can starve IO.
- HTTP lifecycle behavior is uncovered around request release, exception handling, content length, and keep-alive semantics.

### Medium

- Server binds by port only, effectively all interfaces, with no explicit host/interface control.
- No read or idle timeouts are configured.
- Event-loop paths use stdout/`printStackTrace`, risking blocking IO and inconsistent logging.
- `HttpClient` creates a new event loop per request and blocks synchronously.
- `stop` cancels `closeFuture` instead of closing the bound channel.
- Predicate and WebSocket behavior is uncovered.

### Low

- Debug logging can expose request or response payloads.

## Fix Steps

1. Move root handler coroutine execution off Netty event loops onto an application dispatcher.
2. Add focused HTTP lifecycle tests for request release, exception responses, content length, and keep-alive.
3. Add explicit bind host/interface configuration while preserving existing defaults where needed.
4. Configure read/idle timeout handlers and define timeout response behavior.
5. Replace stdout and `printStackTrace` with project logging.
6. Rework `HttpClient` to reuse event loop resources and avoid per-request blocking.
7. Change shutdown to close the bound channel and gracefully terminate Netty groups.
8. Define and test predicate matching and WebSocket routing behavior.
9. Redact or gate debug payload logging.

## Verification

- Run `./gradlew :komok-server-core:test`.
- Add self-contained JUnit 5 tests using Kotlin-specific assertions where applicable.
- Cover keep-alive, content length, exceptions, timeout handling, shutdown, WebSocket routing, and predicate matching.
- Verify no event-loop path performs blocking logging or application handler execution.
