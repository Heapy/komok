package io.heapy.komok.tech.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Owns the root coroutine scope (lives as long as the application), the binding-graph
 * edge store, and the root [Realm]. Coroutines-first: the only realization primitive is
 * the suspend [binding]; there is no non-suspend variant.
 *
 * @param dispatcher dispatcher for realization (default [Dispatchers.Default]).
 *   Overridable for applications and tests.
 * @param rootContext extra always-present root `CoroutineContext` for realm scopes — the
 *   bootstrap installs the root logging context element here (e.g. a
 *   `CoroutineLoggingElement`) so logging providers do not crash. `KomokContext` is
 *   **not** placed here; it flows only as a context parameter.
 */
class KomokRuntime(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    rootContext: CoroutineContext = EmptyCoroutineContext,
) : AutoCloseable {
    private val rootJob = SupervisorJob()
    internal val baseContext: CoroutineContext = dispatcher + rootContext
    private val edgeStore = EdgeStore()
    private val knownBindings = KomokConcurrentSet<BindingId>()

    internal val rootRealm: Realm =
        Realm(runtime = this, parent = null, parentJob = rootJob)

    internal fun recordEdge(from: BindingId, to: BindingId) {
        edgeStore.add(from, to)
    }

    /** Snapshot of binding-graph edges recorded so far (a fact of execution). */
    fun edges(): Set<Edge> =
        edgeStore.snapshot()

    /**
     * Every [BindingId] that has entered the realization boundary — including bindings
     * with no recorded edges (a realized leaf nobody depends on is still a vertex).
     */
    fun bindings(): Set<BindingId> =
        knownBindings.snapshot()

    /** Public read handle for the root realm. */
    fun rootHandle(): RealmHandle =
        RealmHandle(this, rootRealm)

    /**
     * The realization boundary, emitted into each provider body by the compiler. In order: record the caller edge (before any cache
     * lookup, so memo-hits still record), detect cycles against the caller-stack, then
     * resolve once per realm — computing [body] on realm-scope under the entered context.
     */
    context(_: KomokContext)
    suspend fun <T> binding(
        id: BindingId,
        body: suspend context(KomokContext) () -> T,
    ): T {
        val ctx = currentKomokContext()
        realized(knownBindings.add(id))
        val caller = ctx.callerStack.lastOrNull()
        if (caller != null) {
            recordEdge(caller, id)
        }
        if (id in ctx.callerStack) {
            throw KomokCycleException(ctx.callerStack + id)
        }
        val entered = ctx.enter(id)
        @Suppress("UNCHECKED_CAST")
        return ctx.realm.resolve(id, entered, body) as T
    }

    override fun close() {
        rootRealm.close()
        rootJob.cancel()
    }
}

/**
 * Consumes a provider value that was read only for its realization side effect.
 * Generated `realizeAll()` wraps every provider call in this sink so the discarded
 * results stay compliant with `-Xreturn-value-checker=full`.
 */
fun realized(value: Any?) {
    // the value is intentionally ignored; realization memoized it in the realm.
}
