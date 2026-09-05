package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.heapy.komok.tech.runtime.ksp.wrapper.WrapperPlugin
import io.heapy.komok.tech.runtime.ksp.wrapper.WrapperRegistry

/** A decoratable service interface (a provider return type carrying a wrapper annotation). */
internal class WrappedType(
    val declaration: KSClassDeclaration,
    val methods: List<WrappedMethod>,
    val properties: List<WrappedProperty>,
) {
    val fqn: String = declaration.qualifiedName!!.asString()
    val packageName: String = declaration.packageName.asString()
    val simpleName: String = declaration.simpleName.asString()
}

/** One wrapper applied to a method: the discovered [plugin] and the effective annotation. */
internal class AppliedWrapper(
    val plugin: WrapperPlugin,
    val annotation: KSAnnotation,
)

/**
 * One interface method and its applied wrappers in chain order (method-level annotation
 * wins over the type-level one, per plugin).
 */
internal class WrappedMethod(
    val declaration: KSFunctionDeclaration,
    val applied: List<AppliedWrapper>,
) {
    val name: String = declaration.simpleName.asString()
    val isWrapped: Boolean = applied.isNotEmpty()
}

/** One interface property that a decorator/mock must implement. */
internal class WrappedProperty(
    val declaration: KSPropertyDeclaration,
) {
    val name: String = declaration.simpleName.asString()
}

/** Decoratable interface return types across all modules, deduplicated by FQN. */
internal fun collectWrappedTypes(
    models: List<ModuleModel>,
    registry: WrapperRegistry,
): List<WrappedType> {
    val typesByFqn = LinkedHashMap<String, KSClassDeclaration>()
    models.forEach { model ->
        model.providers.forEach { provider ->
            val declaration = provider.returnType?.resolve()?.declaration as? KSClassDeclaration
                ?: return@forEach
            if (declaration.classKind != ClassKind.INTERFACE) return@forEach
            if (!declaration.carriesAnyWrapper(registry)) return@forEach
            typesByFqn.putIfAbsent(declaration.qualifiedName!!.asString(), declaration)
        }
    }
    return typesByFqn.values.map { it.toWrappedType(registry) }
}

/** Explicit `@GenerateMock(Foo::class)` opt-ins for otherwise undiscovered mock types. */
internal fun collectGeneratedMockTypes(
    resolver: Resolver,
    registry: WrapperRegistry,
    logger: KSPLogger,
): List<WrappedType> {
    val typesByFqn = LinkedHashMap<String, KSClassDeclaration>()
    resolver
        .getSymbolsWithAnnotation(WellKnownTypes.GENERATE_MOCK_ANNOTATION)
        .flatMap { annotated ->
            annotated.annotations
                .filter { annotation ->
                    annotation.annotationType.resolve().declaration.qualifiedName?.asString() ==
                        WellKnownTypes.GENERATE_MOCK_ANNOTATION
                }
                .flatMap { annotation -> annotation.classListArgument("value") }
        }
        .forEach { declaration ->
            val fqn = declaration.qualifiedName?.asString()
            if (fqn == null) {
                logger.error("@GenerateMock target must have a qualified name.", declaration)
                return@forEach
            }
            if (declaration.classKind != ClassKind.INTERFACE) {
                logger.error(
                    "@GenerateMock can currently generate mocks only for interfaces; $fqn is ${declaration.classKind}.",
                    declaration,
                )
                return@forEach
            }
            val functions = declaration.getDeclaredFunctions()
                .filter { !it.isConstructor() }
                .toList()
            val properties = declaration.getDeclaredProperties().toList()
            if (functions.isEmpty() && properties.isEmpty()) {
                logger.error("@GenerateMock target $fqn must declare at least one mockable member.", declaration)
                return@forEach
            }
            typesByFqn.putIfAbsent(fqn, declaration)
        }

    return typesByFqn.values.map { it.toWrappedType(registry) }
}

private fun KSClassDeclaration.carriesAnyWrapper(registry: WrapperRegistry): Boolean =
    registry.plugins.any { findWrapperAnnotation(it) != null } ||
        getDeclaredFunctions().any { fn ->
            registry.plugins.any { fn.findWrapperAnnotation(it) != null }
        }

private fun KSClassDeclaration.toWrappedType(registry: WrapperRegistry): WrappedType {
    // Default (non-abstract) interface methods are included: the decorator must delegate
    // them too, or an implementation's override would be silently replaced by the
    // interface default when the value is decorated.
    val methods = getDeclaredFunctions()
        .filter { !it.isConstructor() }
        .map { fn ->
            val applied = registry.plugins.mapNotNull { plugin ->
                val annotation = fn.findWrapperAnnotation(plugin) ?: findWrapperAnnotation(plugin)
                annotation?.let { AppliedWrapper(plugin, it) }
            }
            WrappedMethod(fn, applied)
        }
        .toList()
    val properties = getDeclaredProperties()
        .map { WrappedProperty(it) }
        .toList()
    return WrappedType(this, methods, properties)
}

private fun KSAnnotated.findWrapperAnnotation(plugin: WrapperPlugin): KSAnnotation? =
    annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == plugin.annotationFqn
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
