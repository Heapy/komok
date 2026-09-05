package io.heapy.komok.tech.runtime

/**
 * Marks a `suspend` function inside a [io.heapy.komok.tech.runtime.Module] as a startup hook.
 *
 * Startup hooks run after forced realization of their providers, ordered by
 * `(module-topology dependencies-first, phase)`. A failing hook triggers
 * compensation: already-run hooks are rolled back via the reverse [Shutdown]
 * pass, then boot fails fast.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Startup
