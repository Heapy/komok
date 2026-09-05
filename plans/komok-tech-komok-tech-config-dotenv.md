# komok-tech/komok-tech-config-dotenv Plan

## Scope

Dotenv provider parsing, traversal behavior, public dependency declarations, working-directory sensitivity, and parsing performance.

## Severity-Ranked Issues

### High

- The module exposes runtime types in its public API while declaring the runtime dependency as `implementation`.

### Medium

- Traversal and parsing are awkward to test because behavior depends on process current working directory.

### Low

- Comment matching only handles `# ` and misses common dotenv comment forms.
- Dotenv files are reread from scratch, which can add unnecessary I/O for repeated lookups.

## Fix Steps

1. Promote public runtime dependencies to `api` or hide runtime types behind module-owned abstractions.
2. Add an injectable base directory or file-system abstraction for deterministic tests.
3. Expand parser support for common comment forms and quote/escape cases.
4. Cache parsed dotenv content with clear invalidation semantics if runtime reload is needed.
5. Add tests for traversal, missing files, comments, quoted values, invalid lines, and precedence.

## Verification

- Run `./gradlew :komok-tech:komok-tech-config-dotenv:test`.
- Confirm tests do not depend on the developer process working directory.
