package io.heapy.komok.tech.runtime

/**
 * The single source of truth for "where am I in the graph". Carries the current
 * [Realm] (which owns memoized values) and the caller-stack used for cycle detection.
 */
class KomokContext internal constructor(
    internal val realm: Realm,
    val callerStack: List<BindingId>,
) {
    internal fun enter(id: BindingId): KomokContext =
        KomokContext(realm, callerStack + id)
}

/**
 * Bridge so generated overrides can be written `context(_: KomokContext)` and still read
 * the current context without tripping the context-parameter-name-mismatch warning under
 * `allWarningsAsErrors`.
 */
context(ctx: KomokContext)
fun currentKomokContext(): KomokContext = ctx
