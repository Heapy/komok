package io.heapy.komok.tech.decorators.ksp.api

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import kotlin.reflect.KClass

interface DecoratorPlugin {
    /**
     * Returns the annotation types this plugin is interested in.
     * Methods/classes marked with these annotations will be processed by this plugin.
     */
    fun getAnnotationTypes(): List<KClass<out Annotation>>

    /**
     * Returns a list of required context parameters for the method.
     * KSP Plugin will validate that these parameters exist on the decorated method.
     */
    fun getRequiredContextParams(): List<ContextParam> {
        return emptyList()
    }

    /**
     * Returns the fully qualified name of the wrapping function to use.
     * The number suffix (0-21) indicates the number of parameters supported.
     */
    fun getWrappingFunctionFqn(paramCount: Int): String

    /**
     * Generates code for the decorated methods/classes.
     *
     * @param decoratedElements All methods/classes decorated with this plugin's annotations
     * @param codeGenerator The KSP code generator
     * @param decoratorClassName The name of the generated decorator class
     */
    fun generateCode(
        decoratedElements: List<DecoratedElement>,
        codeGenerator: CodeGenerator,
        decoratorClassName: String,
    ) {
    }
}

data class ContextParam(
    val name: String,
    val type: String,
    val isRequired: Boolean = true
)

sealed class DecoratedElement {
    data class Method(
        val declaration: KSFunctionDeclaration,
        val annotations: List<KSAnnotation>,
        val containingClass: KSClassDeclaration
    ) : DecoratedElement()

    data class Class(
        val declaration: KSClassDeclaration,
        val annotations: List<KSAnnotation>
    ) : DecoratedElement()
}

data class DecoratorContext(
    val originalMethodName: String,
    val originalClassName: String,
    val parameters: List<MethodParam>,
    val returnType: KSType?,
    val isSuspending: Boolean
)

data class MethodParam(
    val name: String,
    val type: KSType,
    val index: Int
)
