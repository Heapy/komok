package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo

class KomokSymbolProcessor(
    private val propertySelector: PropertySelector = publicOpenProperty,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(
        resolver: Resolver,
    ): List<KSAnnotated> {
        val rootDeclarations = resolver
            .getSymbolsWithAnnotation("io.heapy.komok.tech.di.lib.Module")
            .filterIsInstance<KSClassDeclaration>()
            .toList()
        val graph = getDeclarationsWithDependencies(rootDeclarations)
        val sortedGraph = graph.sorted()
        val resolvedGraph = graph.resolveAllDependencies()
        val rootGraph = resolvedGraph.filter { (module, _) -> module in rootDeclarations }

        rootGraph
            .forEach { (module, moduleDependencies) ->
                val className = module.simpleName.asString()
                val packageName = module.packageName.asString()

                val properties = module
                    .getAllProperties()
                    .filter(propertySelector::test)
                    .toList()

                val builderClass = generateBuilderClass(
                    module = module,
                    moduleDependencies = moduleDependencies,
                    properties = properties,
                )

                val builderFunction = generateBuilderFunction(
                    graph = resolvedGraph,
                    sortedGraph = sortedGraph,
                    module = module,
                    moduleDependencies = moduleDependencies,
                )

                val flattenModuleClass = generateFlattenModuleClass(
                    graph = resolvedGraph,
                    sortedGraph = sortedGraph,
                    module = module,
                    moduleDependencies = moduleDependencies,
                )

                val flattenModuleFunction = generateFlattenModuleFunction(
                    graph = resolvedGraph,
                    sortedGraph = sortedGraph,
                    module = module,
                    moduleDependencies = moduleDependencies,
                )

                val overrideClass = generateOverrideClass(
                    module = module,
                    properties = properties,
                )

                val file = FileSpec
                    .builder(
                        packageName,
                        "${className}Factory",
                    )
                    .addType(overrideClass)
                    .addType(flattenModuleClass)
                    .addFunction(flattenModuleFunction)
                    .addType(builderClass)
                    .addFunction(builderFunction)
                    .build()

                file.writeTo(
                    codeGenerator,
                    dependencies(
                        module,
                        moduleDependencies,
                    ),
                )
            }

        return listOf()
    }
}
