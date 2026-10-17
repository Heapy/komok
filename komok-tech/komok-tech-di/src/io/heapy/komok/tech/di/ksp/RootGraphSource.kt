package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

fun Map<KSClassDeclaration, List<KSClassDeclaration>>.resolveAllDependencies(): Map<KSClassDeclaration, List<KSClassDeclaration>> {
    // This function collects all dependencies for a given element
    fun collectDependencies(
        element: KSClassDeclaration,
        graph: Map<KSClassDeclaration, List<KSClassDeclaration>>,
        visited: MutableSet<KSClassDeclaration>
    ): List<KSClassDeclaration> {
        if (visited.contains(element)) return emptyList()
        visited.add(element)
        val dependencies = mutableListOf<KSClassDeclaration>()
        graph[element]?.forEach { dep ->
            dependencies.add(dep)
            dependencies.addAll(
                collectDependencies(
                    dep,
                    graph,
                    visited
                )
            )
        }
        return dependencies.distinct() // Remove duplicates if any
    }

    val result = mutableMapOf<KSClassDeclaration, List<KSClassDeclaration>>()
    val visited = mutableSetOf<KSClassDeclaration>()

    this.keys.forEach { element ->
        visited.clear()
        result[element] = collectDependencies(
            element,
            this,
            visited
        )
    }

    return result
}
