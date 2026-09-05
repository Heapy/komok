package io.heapy.komok.tech.runtime.ksp

import java.util.LinkedList
import java.util.Queue

/** Raised when the module graph (`module -> dependencies`) contains a cycle. */
internal class ModuleCycleException(
    message: String,
) : RuntimeException(message)

/**
 * Kahn topological sort of a `node -> dependencies` graph. Returns nodes in root-first
 * order (a node before its dependencies); reverse it for deps-first construction order.
 * Throws [ModuleCycleException] if the graph has a cycle.
 *
 * Generic (over the node type) per the runtime-foundation plan, so the direction and
 * cycle behavior can be fixed by tests independently of KSP types.
 */
internal fun <T> Map<T, List<T>>.topologicalSort(): List<T> {
    val graph = this
    val inDegree = mutableMapOf<T, Int>().apply {
        for (node in graph.keys) {
            putIfAbsent(node, 0)
            for (neighbor in graph[node] ?: emptySet()) {
                put(neighbor, getOrDefault(neighbor, 0) + 1)
            }
        }
    }

    val queue: Queue<T> = LinkedList()
    for ([node, degree] in inDegree) {
        if (degree == 0) {
            queue.offer(node)
        }
    }

    val sortedList = mutableListOf<T>()
    while (queue.isNotEmpty()) {
        val node = queue.poll()
        sortedList.add(node)
        for (neighbor in graph[node] ?: emptySet()) {
            inDegree[neighbor] = inDegree[neighbor]!! - 1
            if (inDegree[neighbor] == 0) {
                queue.offer(neighbor)
            }
        }
    }

    if (sortedList.size != graph.size) {
        throw ModuleCycleException(
            "Module graph has at least one cycle, topological sorting not possible: $graph",
        )
    }

    return sortedList
}
