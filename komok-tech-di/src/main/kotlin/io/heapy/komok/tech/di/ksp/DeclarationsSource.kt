package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

fun getDeclarationsWithDependencies(
    rootDeclarations: List<KSClassDeclaration>,
): Map<KSClassDeclaration, List<KSClassDeclaration>> {
    fun resolveDependencies(
        declarations: List<KSClassDeclaration>,
    ): Map<KSClassDeclaration, List<KSClassDeclaration>> {
        val graph = mutableMapOf<KSClassDeclaration, List<KSClassDeclaration>>()

        fun resolveDependencies(
            declaration: KSClassDeclaration,
        ) {
            val primaryConstructor = declaration.primaryConstructor
                ?: error("DR: Primary constructor not found for ${declaration.simpleName.asString()}")

            val constructorClasses = primaryConstructor.parameters
                .map { parameter ->
                    parameter.type.resolve()
                        .declaration as KSClassDeclaration
                }

            graph[declaration] = constructorClasses

            constructorClasses.forEach { constructorClass ->
                if (constructorClass !in graph) {
                    resolveDependencies(constructorClass)
                }
            }
        }

        declarations.forEach(::resolveDependencies)

        return graph
    }

    return resolveDependencies(rootDeclarations)
}
