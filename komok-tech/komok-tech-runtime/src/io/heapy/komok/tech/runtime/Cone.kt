package io.heapy.komok.tech.runtime

/**
 * The cone of a set of [overridden] bindings: the overridden bindings themselves plus
 * every binding that transitively depends on one of them, computed from the recorded
 * [edges]. Pure and non-suspend — evaluated once at fork time.
 *
 * An edge `from -> to` means `from` depends on `to`, so when `to` is overridden, `from`
 * must be recomputed; the cone is the transitive closure over reversed edges.
 */
internal fun computeCone(
    overridden: Set<BindingId>,
    edges: Set<Edge>,
): Set<BindingId> {
    val dependents = HashMap<BindingId, MutableList<BindingId>>()
    for (edge in edges) {
        dependents.getOrPut(edge.to) { mutableListOf() }.add(edge.from)
    }

    val cone = HashSet<BindingId>()
    val stack = ArrayDeque(overridden)
    while (stack.isNotEmpty()) {
        val id = stack.removeLast()
        if (cone.add(id)) {
            dependents[id]?.forEach(stack::addLast)
        }
    }
    return cone
}
