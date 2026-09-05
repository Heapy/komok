package io.heapy.komok.tech.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A memoization scope and overlay node. A [Realm] owns a concurrent map of [Cell]s and a
 * child coroutine scope ([SupervisorJob]) on which binding computations run. The root
 * realm has no [parent]; a fork realm overlays a parent with [overrides] and a [coneSet].
 *
 * Memoization is strictly per-realm. A binding computation runs on **this realm's
 * scope**, not the caller's coroutine, so cancelling a reader cancels only its `await`,
 * never the shared computation, and a failing binding does not cascade to its siblings.
 * Failure is memoized terminally and re-thrown.
 *
 * Resolution under realize-then-fork (policy B): an overridden binding is computed
 * locally; an already-completed non-cone binding is shared by reference from the closest
 * ancestor that holds it (the walk stops at any realm whose cone contains the binding);
 * otherwise the binding is computed locally. The child never calls `parent.resolve` on a
 * miss and never returns an ancestor value for a cone binding.
 */
@OptIn(ExperimentalAtomicApi::class)
class Realm internal constructor(
    val runtime: KomokRuntime,
    internal val parent: Realm?,
    parentJob: Job,
    private val overrides: Map<BindingId, BindingOverride<*>> = emptyMap(),
    private val coneSet: Set<BindingId> = emptySet(),
) {
    private val job = SupervisorJob(parentJob)
    private val scope = CoroutineScope(runtime.baseContext + job)
    private val cells = KomokConcurrentMap<BindingId, Cell>()
    private val realizationOrder = KomokConcurrentList<BindingId>()
    private val closed = AtomicBoolean(false)
    private val closedIds = KomokConcurrentSet<BindingId>()

    internal suspend fun resolve(
        id: BindingId,
        entered: KomokContext,
        body: suspend context(KomokContext) () -> Any?,
    ): Any? {
        while (true) {
            when (val cell = cells[id]) {
                is Cell.Completed -> return cell.value
                is Cell.Failed -> throw cell.error
                is Cell.InProgress -> return cell.deferred.await()
                null -> {
                    // Local-first: a value memoized in this realm always wins. Only when no
                    // local cell exists may we share an ancestor's value — and only for a
                    // non-cone binding some ancestor has already completed, with every
                    // intermediate realm's cone respected ([resolvedInstance] walks the
                    // overlay chain, so a fork-of-fork shares grandparent values too).
                    // Checking ancestors before the local cell would let a parent that
                    // completes a non-cone binding *after* the fork silently replace the
                    // child's own instance.
                    val parentRealm = parent
                    if (parentRealm != null && id !in coneSet) {
                        val inherited = parentRealm.resolvedInstance(id)
                        if (inherited != null) {
                            return inherited.value
                        }
                    }
                    val deferred = scope.async(start = CoroutineStart.LAZY) {
                        try {
                            val value = computeValue(id, entered, body)
                            cells.put(id, Cell.Completed(value))
                            realized(realizationOrder.add(id))
                            if (closed.load()) {
                                // The realm closed while this body was completing — the
                                // close() sweep has already run, so release the late value
                                // here (closeOwned dedups against the sweep).
                                closeOwned(id)
                            }
                            value
                        } catch (t: Throwable) {
                            // Failure is terminal — including a CancellationException the
                            // provider throws as its own failure. Only close-triggered
                            // cancellation leaves the cell in-progress: the realm is dead
                            // and awaiting readers surface the cancellation anyway.
                            if (t !is CancellationException || !closed.load()) {
                                cells.put(id, Cell.Failed(t))
                            }
                            throw t
                        }
                    }
                    if (cells.putIfAbsent(id, Cell.InProgress(deferred)) == null) {
                        // We won the race: launch on realm-scope and await our own result.
                        deferred.start()
                        return deferred.await()
                    }
                    // We lost: discard the unstarted computation and re-read the cell.
                    deferred.cancel()
                }
            }
        }
    }

    private suspend fun computeValue(
        id: BindingId,
        entered: KomokContext,
        body: suspend context(KomokContext) () -> Any?,
    ): Any? =
        when (val override = overrides[id]) {
            is BindingOverride.Value -> override.value
            is BindingOverride.Provider -> override.thunk(entered)
            null -> body(entered)
        }

    /**
     * Creates a child realm overlaying this one with [overrides] and a precomputed [cone].
     * Ancestor overrides are inherited (child wins on conflict): a grandchild that has to
     * compute a binding its parent overrode but never realized must still apply that
     * override — the child realm's world is the parent's world plus its own replacements.
     */
    internal fun fork(
        overrides: Map<BindingId, BindingOverride<*>>,
        cone: Set<BindingId>,
    ): Realm =
        Realm(
            runtime = runtime,
            parent = this,
            parentJob = job,
            overrides = this.overrides + overrides,
            coneSet = cone,
        )

    /**
     * The completed value this realm resolves [id] to — locally if memoized here,
     * otherwise the closest ancestor's value for a binding outside every intermediate
     * cone — or `null` if not yet realized. Shared by [resolve] (overlay-chain sharing)
     * and introspection (`isShared`); does not compute.
     */
    internal fun resolvedInstance(id: BindingId): Cell.Completed? {
        val local = cells[id]
        if (local is Cell.Completed) {
            return local
        }
        val parentRealm = parent
        if (parentRealm != null && id !in coneSet) {
            return parentRealm.resolvedInstance(id)
        }
        return null
    }

    /**
     * Cancels this realm's in-flight bindings and closes the [AutoCloseable] values
     * realized in **this** overlay, in reverse realization order. Parent-shared values
     * (never stored locally) and sibling realms are untouched. Idempotent: a second call
     * is a no-op, so composing realm + runtime close never double-closes a resource.
     *
     * Cancellation does not join: a body already past its last suspension point completes
     * after this sweep — such late values self-close in the computing coroutine (see
     * [resolve]), with [closeOwned] deduplicating the two paths.
     */
    internal fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        job.cancel()
        realizationOrder.snapshot().asReversed().forEach(::closeOwned)
    }

    /** Closes the locally-completed value of [id] at most once, best-effort. */
    private fun closeOwned(id: BindingId) {
        if (!closedIds.add(id)) {
            return
        }
        val value = (cells[id] as? Cell.Completed)?.value
        if (value is AutoCloseable) {
            try {
                value.close()
            } catch (_: Throwable) {
                // best-effort cleanup; a failing close must not block the rest.
            }
        }
    }
}
