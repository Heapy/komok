package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.ClassName

/**
 * A resolved `@Module` class: its [providers] (provider-shape functions) and its
 * module-type constructor [dependencies], plus the generated names derived from it.
 */
internal class ModuleModel(
    val declaration: KSClassDeclaration,
    val providers: List<KSFunctionDeclaration>,
    val dependencies: List<KSClassDeclaration>,
    val startupHooks: List<KSFunctionDeclaration>,
    val shutdownHooks: List<KSFunctionDeclaration>,
) {
    val simpleName: String = declaration.simpleName.asString()
    val packageName: String = declaration.packageName.asString()
    val fqn: String = declaration.qualifiedName!!.asString()

    val baseClassName: ClassName = ClassName(packageName, simpleName)
    val komokClassName: ClassName = ClassName(packageName, "${simpleName}_Komok")
    val keysClassName: ClassName = ClassName(packageName, "${simpleName}Keys")
    val graphApiClassName: ClassName = ClassName(packageName, "${simpleName}GraphApi")

    /** Stable lowerCamel name used for DAG properties and `modules.*` accessors. */
    val propertyName: String = simpleName.replaceFirstChar(Char::lowercaseChar)
}

private const val STARTUP_ANNOTATION = "io.heapy.komok.tech.runtime.Startup"
private const val SHUTDOWN_ANNOTATION = "io.heapy.komok.tech.runtime.Shutdown"

/** Resolve every `@Module` class to a [ModuleModel]. */
internal fun collectModules(resolver: Resolver): List<ModuleModel> {
    val declarations = linkedMapOf<String, KSClassDeclaration>()

    fun visit(declaration: KSClassDeclaration) {
        val fqn = declaration.qualifiedName?.asString() ?: return
        if (declarations.putIfAbsent(fqn, declaration) != null) {
            return
        }
        declaration.primaryConstructor
            ?.parameters
            .orEmpty()
            .mapNotNull { parameter ->
                parameter.type.resolve().declaration as? KSClassDeclaration
            }
            .filter { dependency ->
                dependency.hasAnnotation(WellKnownTypes.MODULE_ANNOTATION)
            }
            .forEach(::visit)
    }

    resolver
        .getSymbolsWithAnnotation(WellKnownTypes.MODULE_ANNOTATION)
        .filterIsInstance<KSClassDeclaration>()
        .forEach(::visit)

    val moduleFqns = declarations.keys

    return declarations.values.map { declaration ->
        val functions = declaration.getDeclaredFunctions().toList()
        val supportsParamWiredProviders = declaration.classKind == ClassKind.INTERFACE
        ModuleModel(
            declaration = declaration,
            providers = functions.filter { it.isProvider(supportsParamWiredProviders) },
            dependencies = declaration.moduleDependencies(moduleFqns),
            startupHooks = functions.filter { it.hasAnnotation(STARTUP_ANNOTATION) },
            shutdownHooks = functions.filter { it.hasAnnotation(SHUTDOWN_ANNOTATION) },
        )
    }
}

private fun KSClassDeclaration.moduleDependencies(
    moduleFqns: Set<String>,
): List<KSClassDeclaration> =
    primaryConstructor
        ?.parameters
        .orEmpty()
        .mapNotNull { parameter ->
            parameter.type.resolve().declaration as? KSClassDeclaration
        }
        .filter { dependency ->
            dependency.qualifiedName?.asString() in moduleFqns
        }

private fun KSFunctionDeclaration.isProvider(
    supportsParamWiredProviders: Boolean,
): Boolean {
    if (isConstructor()) return false
    if (Modifier.SUSPEND !in modifiers) return false
    if (parameters.isNotEmpty() && !supportsParamWiredProviders) return false
    if (getVisibility() != Visibility.PUBLIC) return false
    if (isLifecycleHook()) return false
    val returnType = returnType?.resolve() ?: return false
    val returnFqn = returnType.declaration.qualifiedName?.asString()
    return returnFqn != "kotlin.Unit"
}

private fun KSFunctionDeclaration.isLifecycleHook(): Boolean =
    hasAnnotation(STARTUP_ANNOTATION) || hasAnnotation(SHUTDOWN_ANNOTATION)

private fun KSFunctionDeclaration.hasAnnotation(fqn: String): Boolean =
    annotations.any { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }

/** FQNs appearing more than once in [dependencyFqns] — duplicate module-type ctor params. */
internal fun duplicateModuleTypes(dependencyFqns: List<String>): Set<String> =
    dependencyFqns
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

/**
 * Whether a `@Startup`/`@Shutdown` hook has the required shape: a non-private `suspend`
 * function with no value parameters returning `Unit`. The `context(_: KomokContext)`
 * receiver is intentionally not checked here — KSP 2.3.x does not expose context
 * parameters, so a missing/wrong context receiver still surfaces as a generated-code
 * compile error rather than a KSP diagnostic.
 */
internal fun KSFunctionDeclaration.isLifecycleHookShapeValid(): Boolean {
    if (Modifier.SUSPEND !in modifiers) return false
    if (parameters.isNotEmpty()) return false
    if (getVisibility() == Visibility.PRIVATE) return false
    val returnFqn = returnType?.resolve()?.declaration?.qualifiedName?.asString()
    return returnFqn == "kotlin.Unit"
}
