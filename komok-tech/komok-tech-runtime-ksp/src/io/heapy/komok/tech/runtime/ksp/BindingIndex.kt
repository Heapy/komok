package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toTypeName

private const val INDEX_PACKAGE = "komok.index"
private const val INDEX_CLASS = "__KOMOK__Index__"
private const val INDEX_BINDING_KIND_TOP_LEVEL_FUNCTION = "function"
private const val INDEX_BINDING_KIND_OBJECT_FUNCTION = "objectFunction"

internal data class IndexedBinding(
    val name: String,
    val packageName: String,
    val callableName: String,
    val returnType: TypeName,
    val typeKey: String,
    val objectFqn: String? = null,
    val intoSet: Boolean = false,
) {
    fun encode(): String =
        listOf(
            if (objectFqn == null) INDEX_BINDING_KIND_TOP_LEVEL_FUNCTION else INDEX_BINDING_KIND_OBJECT_FUNCTION,
            name,
            packageName,
            callableName,
            returnType.toString(),
            typeKey,
            objectFqn.orEmpty(),
        ).joinToString("|")
}

internal fun collectCurrentIndexedBindings(
    resolver: Resolver,
): List<IndexedBinding> {
    val serviceBindings = resolver
        .getSymbolsWithAnnotation(WellKnownTypes.SERVICE_ANNOTATION)
        .filterIsInstance<KSFunctionDeclaration>()
        .mapNotNull { it.toIndexedBinding() }
        .toList()
    return serviceBindings + collectProvidersFromBindings(resolver)
}

@OptIn(KspExperimental::class)
internal fun collectClasspathIndexedBindings(
    resolver: Resolver,
): List<IndexedBinding> =
    resolver
        .getDeclarationsFromPackage(INDEX_PACKAGE)
        .flatMap { declaration ->
            declaration.annotations.filter { annotation ->
                annotation.annotationType.resolve().declaration.qualifiedName?.asString() ==
                    WellKnownTypes.BINDING_INDEX_ANNOTATION
            }
        }
        .flatMap { annotation ->
            annotation.stringListArgument("bindings").mapNotNull { encoded ->
                decodeIndexedBinding(encoded, intoSet = false)
            } + annotation.stringListArgument("setContributions").mapNotNull { encoded ->
                decodeIndexedBinding(encoded, intoSet = true)
            }
        }
        .toList()

internal fun generateBindingIndexFile(
    bindings: List<IndexedBinding>,
): FileSpec =
    FileSpec.builder(INDEX_PACKAGE, INDEX_CLASS)
        .addType(
            TypeSpec.classBuilder(INDEX_CLASS)
                .addAnnotation(bindingIndexAnnotation(bindings))
                .build(),
        )
        .build()

private fun KSFunctionDeclaration.toIndexedBinding(): IndexedBinding? {
    if (parentDeclaration != null) {
        return null
    }
    if (getVisibility() != Visibility.PUBLIC) {
        return null
    }
    if (parameters.isNotEmpty()) {
        return null
    }
    val returnType = returnType?.resolve() ?: return null
    val bindingType = explicitBindingType() ?: returnType
    val bindingDeclaration = bindingType.declaration.qualifiedName?.asString() ?: return null
    return IndexedBinding(
        name = simpleName.asString(),
        packageName = packageName.asString(),
        callableName = simpleName.asString(),
        returnType = classNameFromFqn(bindingDeclaration),
        typeKey = bindingType.toTypeName().toString(),
        intoSet = hasAnnotation(WellKnownTypes.INTO_SET_ANNOTATION),
    )
}

internal fun collectProvidersFromBindings(
    resolver: Resolver,
): List<IndexedBinding> =
    resolver
        .getSymbolsWithAnnotation(WellKnownTypes.PROVIDERS_FROM_ANNOTATION)
        .flatMap { annotated ->
            annotated.annotations
                .filter { annotation ->
                    annotation.annotationType.resolve().declaration.qualifiedName?.asString() ==
                        WellKnownTypes.PROVIDERS_FROM_ANNOTATION
                }
                .flatMap { annotation -> annotation.classListArgument("value") }
        }
        .flatMap { declaration -> declaration.toProvidersFromBindings() }
        .toList()

private fun KSClassDeclaration.toProvidersFromBindings(): Sequence<IndexedBinding> {
    if (classKind != ClassKind.OBJECT) {
        return emptySequence()
    }
    val objectFqn = qualifiedName?.asString() ?: return emptySequence()
    return getDeclaredFunctions()
        .asSequence()
        .mapNotNull { function -> function.toObjectIndexedBinding(objectFqn) }
}

private fun KSFunctionDeclaration.toObjectIndexedBinding(
    objectFqn: String,
): IndexedBinding? {
    if (getVisibility() != Visibility.PUBLIC) {
        return null
    }
    if (parameters.isNotEmpty()) {
        return null
    }
    val returnType = returnType?.resolve() ?: return null
    val returnFqn = returnType.declaration.qualifiedName?.asString()
    if (returnFqn == "kotlin.Unit") {
        return null
    }
    val bindingDeclaration = returnFqn ?: return null
    return IndexedBinding(
        name = simpleName.asString(),
        packageName = packageName.asString(),
        callableName = simpleName.asString(),
        returnType = classNameFromFqn(bindingDeclaration),
        typeKey = returnType.toTypeName().toString(),
        objectFqn = objectFqn,
        intoSet = hasAnnotation(WellKnownTypes.INTO_SET_ANNOTATION),
    )
}

private fun bindingIndexAnnotation(
    bindings: List<IndexedBinding>,
): AnnotationSpec {
    val encodedBindings = bindings
        .filterNot { it.intoSet }
        .map { CodeBlock.of("%S", it.encode()) }
        .joinToCode(separator = ", ")
    val encodedSetContributions = bindings
        .filter { it.intoSet }
        .map { CodeBlock.of("%S", it.encode()) }
        .joinToCode(separator = ", ")
    return AnnotationSpec.builder(WellKnownTypes.komokBindingIndex)
        .addMember("bindings = [%L]", encodedBindings)
        .addMember("setContributions = [%L]", encodedSetContributions)
        .build()
}

private fun decodeIndexedBinding(
    encoded: String,
    intoSet: Boolean,
): IndexedBinding? {
    val parts = encoded.split("|")
    if (parts.size !in 6..7) {
        return null
    }
    if (parts[0] != INDEX_BINDING_KIND_TOP_LEVEL_FUNCTION && parts[0] != INDEX_BINDING_KIND_OBJECT_FUNCTION) {
        return null
    }
    return IndexedBinding(
        name = parts[1],
        packageName = parts[2],
        callableName = parts[3],
        returnType = classNameFromFqn(parts[4]),
        typeKey = parts[5],
        objectFqn = parts.getOrNull(6)?.takeIf { it.isNotBlank() },
        intoSet = intoSet,
    )
}

private fun classNameFromFqn(
    fqn: String,
): ClassName =
    ClassName(fqn.substringBeforeLast('.'), fqn.substringAfterLast('.'))

private fun KSFunctionDeclaration.explicitBindingType(): KSType? {
    val annotation = annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == WellKnownTypes.SERVICE_ANNOTATION
    } ?: return null
    return annotation.classArgument("bind")
        ?: annotation.classArgument("value")
}

private fun KSAnnotation.classArgument(name: String): KSType? {
    val value = arguments.firstOrNull { it.name?.asString() == name }?.value ?: return null
    val type = value as? KSType ?: return null
    val fqn = type.declaration.qualifiedName?.asString()
    return if (fqn == "kotlin.Unit") null else type
}

private fun KSAnnotation.classListArgument(name: String): Sequence<KSClassDeclaration> {
    val value = arguments.firstOrNull { it.name?.asString() == name }?.value ?: return emptySequence()
    return when (value) {
        is List<*> -> value.asSequence()
        is Array<*> -> value.asSequence()
        else -> sequenceOf(value)
    }.mapNotNull { type ->
        (type as? KSType)?.declaration as? KSClassDeclaration
    }
}

private fun KSAnnotation.stringListArgument(name: String): Sequence<String> {
    val value = arguments.firstOrNull { it.name?.asString() == name }?.value ?: return emptySequence()
    return when (value) {
        is List<*> -> value.asSequence().filterIsInstance<String>()
        is Array<*> -> value.asSequence().filterIsInstance<String>()
        is String -> sequenceOf(value)
        else -> emptySequence()
    }
}

private fun KSFunctionDeclaration.hasAnnotation(fqn: String): Boolean =
    annotations.any { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }
