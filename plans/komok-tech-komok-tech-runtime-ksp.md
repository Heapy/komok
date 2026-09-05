# komok-tech-runtime-ksp Plan

## Scope

KSP generation, synthetic module discovery, classpath index handling, wrapper/mock generation, diagnostics, and processor performance.

## Severity-Ranked Issues

### High

- Generated stop code lacks `try/finally`, so `runtime.close()` can be skipped when stop logic fails.
- `collectSyntheticModule` returns `null` before considering indexed or provided bindings.
- Classpath `komok.index` files are trusted and merged without an allowlist.
- Processor emits the full graph per module, and `reachableModules` scales as `O(M^2)`.
- Module validation diagnostics and synthetic service diagnostics lack real KSP compile-test coverage.

### Medium

- Indexed generic return types are stripped.
- Module accessor names can collide by `simpleName`.
- Wrappers and mocks ignore inherited members.
- `WrapperPlugin` loads all visible `ServiceLoader` plugins without an allowlist.
- Raw parameter names are joined into `%L` generated call-site code.
- `providerDependencyCalls` and repeated aggregate paths recompute indexes.
- Wrapper validation diagnostics and generated mock negative paths are uncovered.

## Fix Steps

1. Wrap generated stop/shutdown flow so `runtime.close()` runs in `finally`.
2. Rework `collectSyntheticModule` to evaluate local, indexed, and provided bindings before returning.
3. Add explicit allowlist validation for classpath `komok.index` inputs before merging.
4. Change graph emission to per-module output and cache reachable-module calculations.
5. Preserve generic return type metadata in index serialization and generated resolution.
6. Make generated module accessor names collision-resistant, using qualified names or stable suffixes.
7. Include inherited members when generating wrappers and mocks.
8. Restrict `WrapperPlugin` discovery to configured or allowlisted providers.
9. Replace raw `%L` parameter-name call generation with structured KotlinPoet parameters.
10. Cache repeated aggregate/index computations inside the processor round.
11. Add real KSP compile tests for module validation, synthetic service diagnostics, wrapper diagnostics, and mock failure paths.

## Verification

- Run `./gradlew :komok-tech:komok-tech-runtime-ksp:test`.
- Add fixtures for failing stop hook cleanup, indexed/provided synthetic modules, unallowlisted classpath indexes, generic return type preservation, accessor name collisions, inherited wrapper/mock members, plugin allowlist behavior, invalid parameter names, and expected diagnostics.
- For performance-sensitive changes, add a multi-module fixture and assert generation does not re-emit the full graph per module or repeatedly recompute reachable modules.
