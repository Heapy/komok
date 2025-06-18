package io.heapy.komok.tech.decorators.lib

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

/**
 * 1. Find all top-level functions that annotated with Decorated. Include meta annotation, i.e annotations that annotated with Decorated (one or multiple) becomes Decorated annotation,
 * 2. For each found function, using kotlinpoet generate class that wraps it, adds decorator into constructor and all context parameters of function as contructor arguments.
 */
class DecoratorsSymbolProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("io.heapy.komok.tech.decorators.lib.Decorated")
        val invalidSymbols = mutableListOf<KSAnnotated>()

        symbols
            .filterIsInstance<KSFunctionDeclaration>() // Process only function declarations
            .forEach { function ->
                if (!function.validate()) {
                    invalidSymbols.add(function)
                    return@forEach
                }

                val (decoratorType, contextParameters) = collectDecoratorAndContexts(function)

                generateWrapperClass(function, decoratorType, contextParameters)
            }

        return invalidSymbols
    }

    /** Collects the relevant `type` from `@Decorated` and any context parameters from `@DecoratorContext`. */
    private fun collectDecoratorAndContexts(function: KSFunctionDeclaration): Pair<ClassName, List<KClass<*>>> {
        val decoratorAnnotation = function.annotations.firstOrNull { it.shortName.asString() == "Decorated" }
            ?: throw IllegalArgumentException("Function is not annotated with @Decorated")

        // Extract the decorator's type parameter
        val decoratorType = decoratorAnnotation.arguments
            .first { it.name?.asString() == "type" }
            .value as KSType
        val decoratorClassName = decoratorType.toClassName()

        // Collect all @DecoratorContext types from function parameters
        val contextParameters = mutableListOf<KClass<*>>()
        function.parameters.forEach { parameter ->
            parameter.annotations
                .filter { it.shortName.asString() == "DecoratorContext" }
                .flatMap { annotation -> annotation.arguments }
                .forEach { argument ->
                    val types = argument.value as? Array<KClass<*>>
                    if (types != null) {
                        contextParameters.addAll(types)
                    }
                }
        }

        return Pair(decoratorClassName, contextParameters)
    }

    /** Generates a Kotlin class that wraps the function and adds decorators into the constructor. */
    private fun generateWrapperClass(
        function: KSFunctionDeclaration,
        decoratorType: ClassName,
        contextParameters: List<KClass<*>>
    ) {
        val packageName = function.packageName.asString()
        val functionName = function.simpleName.asString()
        val wrapperClassName = "${functionName.capitalize()}Wrapper"

        // Define the class and add a primary constructor with a decorator and context parameters
        val wrapperBuilder = TypeSpec.classBuilder(wrapperClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("decorator", decoratorType)
                    .apply {
                        contextParameters.forEach { type ->
                            addParameter(type.simpleName!!.lowercase(), type)
                        }
                    }
                    .build()
            )
            .addProperty(
                PropertySpec.builder("decorator", decoratorType)
                    .initializer("decorator")
                    .build()
            )
            .apply {
                contextParameters.forEach { type ->
                    addProperty(
                        PropertySpec.builder(type.simpleName!!.lowercase(), type)
                            .initializer(type.simpleName!!.lowercase())
                            .build()
                    )
                }
            }

        // Add the wrapped function call method
        wrapperBuilder.addFunction(
            FunSpec.builder(functionName)
                .addParameters(function.parameters.map {
                    ParameterSpec(it.name!!.asString(), it.type.resolve().toClassName())
                })
                .returns(function.returnType!!.resolve().toClassName())
                .addStatement("// Add call logic here using decorator")
                .build()
        )

        // Write the generated class to the output
        val fileSpec = FileSpec.builder(packageName, wrapperClassName)
            .addType(wrapperBuilder.build())
            .build()


        println(fileSpec.toString())
    }
}
