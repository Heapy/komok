package io.heapy.komok.tech.di.ez.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class KomokEzSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(
        resolver: Resolver,
    ): List<KSAnnotated> {
        val visitor = KomokEzVisitor()

        resolver
            .getSymbolsWithAnnotation("io.heapy.komok.tech.di.ez.lib.Ez")
            .forEach { annotated ->
                annotated.accept(
                    visitor,
                    Unit,
                )
            }

        val functions = visitor.functions
            .map { function ->
                KomokFunctionKtor(
                    function = function,
                )
            }

        val constructors = visitor.classes
            .map { classDeclaration ->
                val ktor = classDeclaration.primaryConstructor

                if (ktor == null) {
                    val message =
                        "${classDeclaration.qualifiedName} is missing a primary constructor"
                    logger.error(message)
                    error(message)
                }

                KomokClassKtor(
                    ktor = ktor,
                    clazz = classDeclaration,
                )
            }

        val providers = constructors + functions

        if (providers.isNotEmpty()) {
            val packageName = providers
                .fold("") { acc, provider ->
                    val current = provider.packageName.asString()

                    when {
                        acc.isEmpty() -> current
                        current.length > acc.length -> acc
                        else -> current
                    }
                }

            val classes = providers.mapIndexed { idx, provider ->
                val returnType = provider.returnType

                TypeSpec
                    .classBuilder("${returnType}Factory$idx")
                    .build()
            }

            val file = FileSpec
                .builder(
                    packageName,
                    "DiEz",
                )
                .apply {
                    addTypes(classes)
                }
                .build()

            file.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(
                    aggregating = true,
                    sources = providers
                        .mapNotNull {
                            it.containingFile
                        }
                        .toSet()
                        .toTypedArray(),
                ),
            )
        }

        return emptyList()
    }
}


sealed interface KomokKtor

data class KomokFunctionKtor(
    val function: KSFunctionDeclaration,
) : KomokKtor

data class KomokClassKtor(
    val ktor: KSFunctionDeclaration,
    val clazz: KSClassDeclaration,
) : KomokKtor

val KomokKtor.packageName: KSName
    get() = when (this) {
        is KomokClassKtor -> clazz.packageName
        is KomokFunctionKtor -> function.packageName
    }

val KomokKtor.containingFile: KSFile?
    get() = when (this) {
        is KomokClassKtor -> clazz.containingFile
        is KomokFunctionKtor -> function.containingFile
    }

val KomokKtor.returnType: String
    get() = when (this) {
        is KomokClassKtor -> clazz.simpleName.asString()
        is KomokFunctionKtor -> function.returnType?.resolve()?.toClassName()?.simpleName
            ?: error("Return type for $this is not inferred")
    }
