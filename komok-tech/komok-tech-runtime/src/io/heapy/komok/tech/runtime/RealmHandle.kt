package io.heapy.komok.tech.runtime

/**
 * Runtime-owned binding of a chosen [Realm] to a root [KomokContext]. The single public
 * (`suspend`) boundary for reading the graph: [read] runs [body] under
 * `context(this.context)` so callers never see [KomokContext] directly.
 */
class RealmHandle internal constructor(
    val runtime: KomokRuntime,
    internal val realm: Realm,
) {
    internal val context: KomokContext =
        KomokContext(realm, emptyList())

    suspend fun <T> read(body: suspend context(KomokContext) () -> T): T =
        body(context)

    /**
     * Creates a child handle overlaying this realm with [overrides]. Non-suspend and does
     * not read the graph: it only snapshots the cone from recorded edges. The same
     * generated module DAG is reused — memoization is strictly per-realm.
     */
    fun fork(overrides: Map<BindingId, BindingOverride<*>>): RealmHandle {
        val cone = computeCone(overrides.keys, runtime.edges())
        return RealmHandle(runtime, realm.fork(overrides, cone))
    }

    /**
     * Whether this handle and [other] currently resolve [id] to the **same instance**
     * (by reference). True for a non-cone binding shared from a common parent; false when
     * either side hasn't realized it or they hold distinct instances. Does not compute.
     */
    fun isShared(id: BindingId, other: RealmHandle): Boolean {
        val here = realm.resolvedInstance(id) ?: return false
        val there = other.realm.resolvedInstance(id) ?: return false
        return here.value === there.value
    }

    /** Cancels this realm's in-flight bindings (closes only this overlay). */
    fun close() {
        realm.close()
    }
}
