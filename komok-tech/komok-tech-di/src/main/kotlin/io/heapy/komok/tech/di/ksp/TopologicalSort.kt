package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.util.*

fun Map<KSClassDeclaration, List<KSClassDeclaration>>.sorted(): List<KSClassDeclaration> {
    val graph = this
    // Map to store the in-degree of each node
    val inDegree = mutableMapOf<KSClassDeclaration, Int>().apply {
        for (node in graph.keys) {
            putIfAbsent(
                node,
                0
            )
            for (neighbor in graph[node]
                ?: emptySet()) {
                put(
                    neighbor,
                    getOrDefault(
                        neighbor,
                        0
                    ) + 1
                )
            }
        }
    }

    // Queue to hold nodes with in-degree 0
    val queue: Queue<KSClassDeclaration> = LinkedList()
    for ((node, degree) in inDegree) {
        if (degree == 0) {
            queue.offer(node)
        }
    }

    val sortedList = mutableListOf<KSClassDeclaration>()
    while (queue.isNotEmpty()) {
        val node = queue.poll()
        sortedList.add(node)

        // Decrease the in-degree of neighboring nodes
        for (neighbor in graph[node]
            ?: emptySet()) {
            inDegree[neighbor] = inDegree[neighbor]!! - 1
            if (inDegree[neighbor] == 0) {
                queue.offer(neighbor)
            }
        }
    }

    // Check for cycles
    if (sortedList.size != graph.size) {
        error("Graph has at least one cycle, topological sorting not possible, $graph")
    }

    return sortedList
}
