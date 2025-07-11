package io.heapy.komok.tech.decorators.lib

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import io.heapy.komok.tech.decorators.ksp.api.ContextParam
import io.heapy.komok.tech.decorators.ksp.api.DecoratedElement
import io.heapy.komok.tech.decorators.ksp.api.DecoratorContext
import io.heapy.komok.tech.decorators.ksp.api.DecoratorPlugin
import io.heapy.komok.tech.decorators.ksp.api.MethodParam
import java.util.*
import kotlin.collections.filter

class DecoratorsSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private val decoratorPlugins: List<DecoratorPlugin> by lazy {
        ServiceLoader.load(DecoratorPlugin::class.java, this.javaClass.classLoader)
            .toList()
            .also { plugins ->
                logger.info("Loaded ${plugins.size} decorator plugins: ${plugins.joinToString { it.javaClass.simpleName }}")
            }
    }

    private val processedSymbols = mutableSetOf<KSAnnotated>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferredSymbols = mutableListOf<KSAnnotated>()

        // Collect all annotation types from plugins
        val annotationTypes = decoratorPlugins.flatMap { it.getAnnotationTypes() }
            .map { it.qualifiedName!! }
            .toSet()

        // Find all symbols with decorator annotations
        val annotatedSymbols = annotationTypes.flatMap { annotationType ->
            resolver.getSymbolsWithAnnotation(annotationType)
        }.distinct()

        // Process each annotated symbol
        annotatedSymbols.forEach { symbol ->
            if (symbol in processedSymbols) return@forEach

            if (!symbol.validate()) {
                deferredSymbols.add(symbol)
                return@forEach
            }

            when (symbol) {
                is KSClassDeclaration -> processClass(symbol, resolver)
                is KSFunctionDeclaration -> processFunction(symbol, resolver)
                else -> logger.warn("Unsupported symbol type: ${symbol.javaClass.simpleName}")
            }

            processedSymbols.add(symbol)
        }

        return deferredSymbols
    }

    private fun processClass(classDeclaration: KSClassDeclaration, resolver: Resolver) {
        // Validate that class is either interface or open class
        if (!validateClass(classDeclaration)) return

        val decoratedElements = mutableListOf<DecoratedElement>()
        val classAnnotations = classDeclaration.annotations.toList()

        // Check if class has decorator annotations
        val hasClassDecorators = hasDecoratorAnnotations(classAnnotations)

        // Process all methods in the class
        classDeclaration.getAllFunctions().forEach { function ->
            val methodAnnotations = function.annotations.toList()
            val hasMethodDecorators = hasDecoratorAnnotations(methodAnnotations)

            // Method decorators override class decorators
            val effectiveAnnotations = if (hasMethodDecorators) {
                methodAnnotations
            } else if (hasClassDecorators) {
                classAnnotations
            } else {
                return@forEach
            }

            // Validate required context params
            if (!validateContextParams(function, effectiveAnnotations)) return@forEach

            decoratedElements.add(
                DecoratedElement.Method(
                    declaration = function,
                    annotations = effectiveAnnotations,
                    containingClass = classDeclaration
                )
            )
        }

        if (decoratedElements.isNotEmpty()) {
            generateDecoratorClass(classDeclaration, decoratedElements, resolver)
        }
    }

    private fun processFunction(function: KSFunctionDeclaration, resolver: Resolver) {
        val containingClass = function.parentDeclaration as? KSClassDeclaration
        if (containingClass == null) {
            logger.error("Function ${function.simpleName.asString()} must be inside a class")
            return
        }

        if (!validateClass(containingClass)) return

        val annotations = function.annotations.toList()
        if (!validateContextParams(function, annotations)) return

        val decoratedElement = DecoratedElement.Method(
            declaration = function,
            annotations = annotations,
            containingClass = containingClass
        )

        generateDecoratorClass(containingClass, listOf(decoratedElement), resolver)
    }

    private fun validateClass(classDeclaration: KSClassDeclaration): Boolean {
        val isInterface = classDeclaration.classKind == ClassKind.INTERFACE
        val isOpenClass = classDeclaration.modifiers.contains(Modifier.OPEN) ||
                          classDeclaration.modifiers.contains(Modifier.ABSTRACT)

        if (!isInterface && !isOpenClass) {
            logger.error(
                "Class ${classDeclaration.qualifiedName?.asString()} must be either an interface or an open/abstract class"
            )
            return false
        }

        return true
    }

    private fun hasDecoratorAnnotations(annotations: List<KSAnnotation>): Boolean {
        val decoratorAnnotationNames = decoratorPlugins.flatMap { it.getAnnotationTypes() }
            .map { it.qualifiedName!! }
            .toSet()

        return annotations.any { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() in decoratorAnnotationNames
        }
    }

    private fun validateContextParams(function: KSFunctionDeclaration, annotations: List<KSAnnotation>): Boolean {
        val requiredParams = getRequiredContextParams(annotations)
        val functionParams = function.parameters.map { param ->
            param.name?.asString() to param.type.resolve().declaration.qualifiedName?.asString()
        }.toMap()

        for (requiredParam in requiredParams) {
            if (!requiredParam.isRequired) continue

            val paramType = functionParams[requiredParam.name]
            if (paramType == null) {
                logger.error(
                    "Function ${function.simpleName.asString()} is missing required parameter '${requiredParam.name}' of type ${requiredParam.type}"
                )
                return false
            }

            if (paramType != requiredParam.type) {
                logger.error(
                    "Function ${function.simpleName.asString()} parameter '${requiredParam.name}' has type $paramType but expected ${requiredParam.type}"
                )
                return false
            }
        }

        return true
    }

    private fun getRequiredContextParams(annotations: List<KSAnnotation>): List<ContextParam> {
        return annotations.flatMap { annotation ->
            val annotationType = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
            decoratorPlugins.filter { plugin ->
                plugin.getAnnotationTypes().any { it.qualifiedName == annotationType }
            }.flatMap { it.getRequiredContextParams() }
        }.distinct()
    }

    private fun generateDecoratorClass(
        classDeclaration: KSClassDeclaration,
        decoratedElements: List<DecoratedElement>,
        resolver: Resolver
    ) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val decoratorClassName = "${className}Decorator"

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = decoratorClassName
        )

        file.use { output ->
            output.write("package $packageName\n\n".toByteArray())

            // Write imports
            val imports = collectImports(classDeclaration, decoratedElements)
            imports.forEach { import ->
                output.write("import $import\n".toByteArray())
            }
            output.write("\n".toByteArray())

            // Generate class header
            val isInterface = classDeclaration.classKind == ClassKind.INTERFACE
            val classHeader = if (isInterface) {
                "class $decoratorClassName(\n    private val delegate: $className\n) : $className"
            } else {
                "class $decoratorClassName(\n    private val delegate: $className\n) : $className()"
            }
            output.write("$classHeader {\n".toByteArray())

            // Generate methods
            decoratedElements.filterIsInstance<DecoratedElement.Method>().forEach { element ->
                generateDecoratedMethod(output, element, packageName)
            }

            output.write("}\n".toByteArray())
        }

        // Allow plugins to generate additional code
        decoratorPlugins.forEach { plugin ->
            val relevantElements = decoratedElements.filter { element ->
                val annotations = when (element) {
                    is DecoratedElement.Method -> element.annotations
                    is DecoratedElement.Class -> element.annotations
                }
                annotations.any { annotation ->
                    plugin.getAnnotationTypes().any {
                        it.qualifiedName == annotation.annotationType.resolve().declaration.qualifiedName?.asString()
                    }
                }
            }

            if (relevantElements.isNotEmpty()) {
                plugin.generateCode(
                    decoratedElements = relevantElements,
                    codeGenerator = codeGenerator,
                    decoratorClassName = decoratorClassName,
                )
            }
        }
    }

    private fun generateDecoratedMethod(
        output: java.io.OutputStream,
        element: DecoratedElement.Method,
        packageName: String
    ) {
        val function = element.declaration
        val annotations = element.annotations

        // Generate method signature
        val modifiers = function.modifiers.filter {
            it != Modifier.ABSTRACT && it != Modifier.OPEN
        }.joinToString(" ")

        val suspendModifier = if (function.modifiers.contains(Modifier.SUSPEND)) "suspend " else ""
        val typeParams = if (function.typeParameters.isNotEmpty()) {
            "<${function.typeParameters.joinToString { it.name.asString() }}> "
        } else ""

        val params = function.parameters.joinToString { param ->
            "${param.name?.asString()}: ${param.type.resolve().declaration.simpleName.asString()}"
        }

        val returnType = function.returnType?.resolve()?.declaration?.simpleName?.asString() ?: "Unit"

        output.write("    override ${modifiers} ${suspendModifier}fun $typeParams${function.simpleName.asString()}($params): $returnType {\n".toByteArray())

        // Find relevant plugin for this method
        val relevantPlugin = decoratorPlugins.find { plugin ->
            annotations.any { annotation ->
                plugin.getAnnotationTypes().any {
                    it.qualifiedName == annotation.annotationType.resolve().declaration.qualifiedName?.asString()
                }
            }
        }

        if (relevantPlugin != null) {
            // Generate decorator context
            val context = createDecoratorContext(function, element.containingClass)
            val requiredParams = relevantPlugin.getRequiredContextParams()

            // Count non-context parameters
            val nonContextParams = function.parameters.filter { param ->
                requiredParams.none { it.name == param.name?.asString() }
            }
            val wrapperFqn = relevantPlugin.getWrappingFunctionFqn(nonContextParams.size)

            // Generate method body with wrapper
            val delegateCall = "delegate.${function.simpleName.asString()}(${
                function.parameters.joinToString { it.name?.asString() ?: "" }
            })"

            output.write("        val context = ${generateContextCreation(context)}\n".toByteArray())

            // Build wrapper call with context params and regular params
            val contextParamNames = requiredParams.joinToString(", ") { it.name }
            val regularParamNames = nonContextParams.joinToString(", ") { it.name?.asString() ?: "" }
            val allWrapperParams = listOf("context", contextParamNames, regularParamNames)
                .filter { it.isNotEmpty() }
                .joinToString(", ")

            output.write("        return $wrapperFqn($allWrapperParams) { $delegateCall }\n".toByteArray())
        } else {
            // Just delegate if no plugin found
            val delegateCall = "delegate.${function.simpleName.asString()}(${
                function.parameters.joinToString { it.name?.asString() ?: "" }
            })"
            output.write("        return $delegateCall\n".toByteArray())
        }

        output.write("    }\n\n".toByteArray())
    }

    private fun createDecoratorContext(
        function: KSFunctionDeclaration,
        containingClass: KSClassDeclaration
    ): DecoratorContext {
        return DecoratorContext(
            originalMethodName = function.simpleName.asString(),
            originalClassName = containingClass.qualifiedName?.asString()
                ?: "",
            parameters = function.parameters.mapIndexed { index, param ->
                MethodParam(
                    name = param.name?.asString()
                        ?: "param$index",
                    type = param.type.resolve(),
                    index = index
                )
            },
            returnType = function.returnType?.resolve(),
            isSuspending = function.modifiers.contains(Modifier.SUSPEND)
        )
    }

    private fun generateContextCreation(context: DecoratorContext): String {
        return """DecoratorContext(
            |            originalMethodName = "${context.originalMethodName}",
            |            originalClassName = "${context.originalClassName}",
            |            parameters = listOf(${context.parameters.joinToString {
                "MethodParam(\"${it.name}\", ${it.type.declaration.simpleName.asString()}::class, ${it.index})"
            }}),
            |            returnType = ${context.returnType?.declaration?.simpleName?.asString() ?: "null"},
            |            isSuspending = ${context.isSuspending}
            |        )""".trimMargin()
    }

    private fun collectImports(
        classDeclaration: KSClassDeclaration,
        decoratedElements: List<DecoratedElement>
    ): Set<String> {
        val imports = mutableSetOf<String>()

        // Add imports for parameter and return types
        decoratedElements.filterIsInstance<DecoratedElement.Method>().forEach { element ->
            element.declaration.parameters.forEach { param ->
                param.type.resolve().declaration.qualifiedName?.asString()?.let { imports.add(it) }
            }
            element.declaration.returnType?.resolve()?.declaration?.qualifiedName?.asString()?.let { imports.add(it) }
        }

        // Add imports for decorator context
        imports.add("io.heapy.komok.tech.decorators.api.DecoratorContext")
        imports.add("io.heapy.komok.tech.decorators.api.MethodParam")

        return imports
    }
}
