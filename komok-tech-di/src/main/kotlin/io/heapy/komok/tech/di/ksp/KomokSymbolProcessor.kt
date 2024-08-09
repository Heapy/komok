package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo

class KomokSymbolProcessor(
    private val propertySelector: PropertySelector = publicOpenProperty,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
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
                try {
                    val className = module.simpleName.asString()
                    val packageName = module.packageName.asString()

                    val properties = module
                        .getAllProperties()
                        .filter(propertySelector::test)
                        .toList()

                    val builderClass = try {
                        generateBuilderClass(
                            logger = logger,
                            module = module,
                            moduleDependencies = moduleDependencies,
                            properties = properties,
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to generate builder class", module)
                        throw e
                    }

                    val builderFunction = try {
                        generateBuilderFunction(
                            graph = resolvedGraph,
                            sortedGraph = sortedGraph,
                            module = module,
                            moduleDependencies = moduleDependencies,
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to generate builder function", module)
                        throw e
                    }

                    val flattenModuleClass = try {
                        generateFlattenModuleClass(
                            graph = resolvedGraph,
                            sortedGraph = sortedGraph,
                            module = module,
                            moduleDependencies = moduleDependencies,
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to generate flatten module class", module)
                        throw e
                    }

                    val flattenModuleFunction = try {
                        generateFlattenModuleFunction(
                            graph = resolvedGraph,
                            sortedGraph = sortedGraph,
                            module = module,
                            moduleDependencies = moduleDependencies,
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to generate flatten module function", module)
                        throw e
                    }

                    val overrideClass = try {
                        generateOverrideClass(
                            module = module,
                            properties = properties,
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to generate override class", module)
                        throw e
                    }

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
                } catch (e: Exception) {
                    logger.error(
                        """
                            Komok DI Failed with error: ${e.message}
                            In ${module.location()}
                        """.trimIndent(),
                        module,
                    )
                    throw e
                }
            }

        return listOf()
    }
}

fun KSNode.location(): String {
    return when (val cached = location) {
        is FileLocation -> """
            ${cached.filePath}:${cached.lineNumber}
        """.trimIndent()
        NonExistLocation -> """
            We are sorry, but the location of this node is not available.
        """.trimIndent()
    }
}
