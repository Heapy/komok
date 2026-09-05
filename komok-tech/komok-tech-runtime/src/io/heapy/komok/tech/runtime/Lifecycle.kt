package io.heapy.komok.tech.runtime

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** Whether a [LifecycleHook] runs during startup or shutdown. */
enum class LifecyclePhase {
    STARTUP,
    SHUTDOWN,
}

/**
 * A registered `@Startup`/`@Shutdown` hook: its owning [moduleId], its [phase], and the
 * [action] that invokes the module method (reading the graph to provide its context).
 */
class LifecycleHook(
    val moduleId: String,
    val phase: LifecyclePhase,
    val action: suspend () -> Unit,
)

/**
 * Collects lifecycle hooks. Generated graph code registers each module's hooks in
 * deps-first module order (and declaration order within a module), so [hooks] is already
 * in startup order.
 */
class LifecycleRegistry {
    private val registered = mutableListOf<LifecycleHook>()

    val hooks: List<LifecycleHook>
        get() = registered.toList()

    fun register(
        moduleId: String,
        phase: LifecyclePhase,
        action: suspend () -> Unit,
    ) {
        registered.add(LifecycleHook(moduleId, phase, action))
    }
}

/**
 * Runs the lifecycle passes over a [LifecycleRegistry]. Order is purely the static module
 * DAG (deps-first) baked into registration order — no realization needed to decide it.
 */
@OptIn(ExperimentalAtomicApi::class)
class Lifecycle(
    private val registry: LifecycleRegistry,
) {
    private val started = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)

    /**
     * Forces each `@Startup` hook in deps-first order. If a hook fails, the already-run
     * `@Startup` hooks are compensated by running the `@Shutdown` hooks of their modules
     * in reverse order (each awaited), then the failure is rethrown (fail-fast). Later
     * hooks do not run. Idempotent: a second call is a no-op.
     */
    suspend fun startup() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        val completed = mutableListOf<LifecycleHook>()
        try {
            registry.hooks
                .filter { it.phase == LifecyclePhase.STARTUP }
                .forEach { hook ->
                    hook.action()
                    completed.add(hook)
                }
        } catch (failure: Throwable) {
            // Compensation IS the teardown for a failed boot: mark the lifecycle stopped
            // so a later stop()/shutdown() (e.g. from a finally block) is a no-op instead
            // of a second teardown that would also fire hooks of never-started modules.
            stopped.store(true)
            try {
                compensate(completed)
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
            throw failure
        }
    }

    /**
     * Runs every `@Shutdown` hook in strict reverse of startup order, awaiting each.
     * Idempotent: a second call is a no-op, so a redundant `stop()` never re-fires hooks.
     */
    suspend fun shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return
        }
        registry.hooks
            .filter { it.phase == LifecyclePhase.SHUTDOWN }
            .asReversed()
            .forEach { hook -> hook.action() }
    }

    private suspend fun compensate(completedStartup: List<LifecycleHook>) {
        val startedModules = completedStartup.mapTo(mutableSetOf()) { it.moduleId }
        registry.hooks
            .filter { it.phase == LifecyclePhase.SHUTDOWN && it.moduleId in startedModules }
            .asReversed()
            .forEach { hook -> hook.action() }
    }
}
