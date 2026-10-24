# komok-app Review Plan

## Scope

Application configuration, authentication/session handling, login flow, entity routes, startup behavior, WebSocket handling, metrics exposure, pagination, database query performance, and test coverage gaps.

## Severity-Ranked Issues

### Critical

- Packaged JWT secret is present in `application.conf`.
- Argon2 `memLimit` is set to `1048576` KB inline in login, creating high resource pressure and denial-of-service risk.

### High

- JWT-vs-cookie auth mismatch after login.
- Hard-coded `UserContext("1")` in entity get/update routes.
- Startup logs expose generated default password and TOTP seed.
- Public WebSocket allows `Long.MAX_VALUE` frame size and echoes `readText`.
- Missing indexes for auth, session, and entity query paths.

### Medium

- Startup tasks launch and return before the server starts accepting traffic.
- Login flow allows user/password/OTP enumeration and has no throttling.
- `X-Forwarded-For` is trusted for session IP binding.
- Metrics endpoint is public.
- Pagination allows unbounded `skip`.

### Low

- Session cookie is missing explicit `SameSite`.

## Coverage Gaps

- Login flow, session handling, `/me`, status pages, entity routes, autoregistration, WIP mapping/HCL2, generators, and Base32.

## Fix Steps

1. Move secrets out of packaged config and require runtime-provided JWT secret.
2. Replace inline Argon2 parameters with bounded, configurable values and tests for acceptable limits.
3. Align post-login auth behavior so JWT/session cookie expectations are consistent.
4. Replace hard-coded `UserContext("1")` with authenticated request context in entity routes.
5. Remove sensitive startup logging for passwords and TOTP seeds.
6. Lock down WebSocket frame size, authentication, and echo behavior.
7. Add indexes for auth, session, and entity query paths.
8. Make startup task completion deterministic before accepting traffic.
9. Normalize login errors, add throttling, and avoid user/password/OTP enumeration.
10. Treat forwarded IP headers as trusted only behind configured proxies.
11. Restrict metrics exposure.
12. Bound pagination `skip`.
13. Add `SameSite` to the session cookie.
14. Backfill focused tests for the listed coverage gaps.

## Verification

- Run `./gradlew :komok-app:test`.
- Add regression tests for auth/session/login/entity route behavior.
- Verify startup no longer logs sensitive credentials.
- Verify unauthenticated access is rejected for protected routes, WebSocket, and metrics.
- Verify database migrations include the required indexes.
- Verify pagination rejects or caps excessive `skip` values.
