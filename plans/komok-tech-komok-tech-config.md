# komok-tech/komok-tech-config Plan

## Scope

Configuration runtime graph integration, environment/property overrides, dotenv precedence, public dependency declarations, and test coverage.

## Severity-Ranked Issues

### High

- The module exposes runtime types in its public API while declaring the runtime dependency as `implementation`.
- Tests or helpers that use `System.setProperty` risk leaking process-global state between tests.

### Medium

- `.env` values can override process environment values, which may surprise production deployments and weaken operator-controlled configuration.
- HOCON and runtime mock behavior have limited coverage.
- Environment resolution performs repeated broad scans and may scale poorly with larger environments.

## Fix Steps

1. Promote public runtime dependencies to `api` or hide runtime types behind module-owned abstractions.
2. Wrap `System.setProperty` tests with strict setup/teardown helpers that restore previous values.
3. Define and test explicit precedence between process env, system properties, HOCON, and `.env` values.
4. Add HOCON parsing and runtime mock integration tests.
5. Cache or narrow environment lookups to avoid repeated full scans.

## Verification

- Run `./gradlew :komok-tech:komok-tech-config:test`.
- Add dependency exposure checks by compiling a tiny consumer fixture or Gradle TestKit scenario.
