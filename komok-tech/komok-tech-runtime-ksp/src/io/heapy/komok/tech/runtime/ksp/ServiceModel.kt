package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class SyntheticModuleModel(
    val packageName: String,
    val simpleName: String,
    val fqn: String,
    val providers: List<ServiceProviderModel>,
) {
    val komokClassName: ClassName = ClassName(packageName, "${simpleName}_Komok")
    val keysClassName: ClassName = ClassName(packageName, "${simpleName}Keys")
    val graphApiClassName: ClassName = ClassName(packageName, "${simpleName}GraphApi")
    val providerScopeClassName: ClassName = ClassName(packageName, "${simpleName}ProviderScope")
    val forkScopeClassName: ClassName = ClassName(packageName, "${simpleName}ForkScope")
    val propertyName: String = simpleName.replaceFirstChar(Char::lowercaseChar)
}

internal sealed class ServiceProviderModel(
    val name: String,
    val returnType: TypeName,
    val typeKey: String,
    val dependencies: List<ServiceDependency>,
    val dependencyProviders: List<String>,
    val intoSet: Boolean,
    val origin: KSAnnotated?,
) {
    class Function(
        val declaration: KSFunctionDeclaration,
        name: String,
        returnType: TypeName,
        typeKey: String,
        dependencies: List<ServiceDependency>,
        dependencyProviders: List<String>,
        intoSet: Boolean,
    ) : ServiceProviderModel(name, returnType, typeKey, dependencies, dependencyProviders, intoSet, declaration)

    class Constructor(
        val declaration: KSClassDeclaration,
        val startupHooks: List<KSFunctionDeclaration>,
        val shutdownHooks: List<KSFunctionDeclaration>,
        name: String,
        returnType: TypeName,
        typeKey: String,
        dependencies: List<ServiceDependency>,
        dependencyProviders: List<String>,
    ) : ServiceProviderModel(name, returnType, typeKey, dependencies, dependencyProviders, intoSet = false, declaration)

    class AggregateSet(
        val elementType: TypeName,
        val elementTypeKey: String,
        name: String,
        returnType: TypeName,
        typeKey: String,
        dependencyProviders: List<String>,
        origin: KSAnnotated?,
    ) : ServiceProviderModel(name, returnType, typeKey, emptyList(), dependencyProviders, intoSet = false, origin)

    class ExternalFunction(
        val packageName: String,
        val functionName: String,
        val objectClassName: ClassName?,
        name: String,
        returnType: TypeName,
        typeKey: String,
        intoSet: Boolean,
    ) : ServiceProviderModel(name, returnType, typeKey, emptyList(), emptyList(), intoSet, null)

    val signatureDescriptor: String =
        dependencies.joinToString(prefix = "(", postfix = ")") { it.typeKey }.takeIf { it != "()" }.orEmpty()
}

internal class ServiceDependency(
    val name: String,
    val type: TypeName,
    val typeKey: String,
    val setElementType: TypeName?,
    val setElementTypeKey: String?,
)

private class RawServiceProvider(
    val name: String,
    val returnType: TypeName,
    val typeKey: String,
    val dependencies: List<ServiceDependency>,
    val intoSet: Boolean,
    val toModel: (List<String>) -> ServiceProviderModel,
    val origin: KSAnnotated?,
)

private class RawAggregateSet(
    val name: String,
    val returnType: TypeName,
    val typeKey: String,
    val elementType: TypeName,
    val elementTypeKey: String,
    val contributors: List<RawServiceProvider>,
    val origin: KSAnnotated?,
)

internal fun collectSyntheticModule(
    resolver: Resolver,
    moduleFqn: String?,
    logger: KSPLogger,
    indexedBindings: List<IndexedBinding>,
): SyntheticModuleModel? {
    val localProviders = collectRawServiceProviders(resolver, logger)
    if (localProviders.isEmpty()) {
        return null
    }
    val providers = localProviders + indexedBindings.map { it.toRawServiceProvider() }
    if (moduleFqn.isNullOrBlank()) {
        return null
    }
    if ("." !in moduleFqn) {
        logger.error(
            "@Service declarations require KSP arg `komok.providers.module=<package>.<SyntheticModule>`.",
        )
        return null
    }

    val typeIndex = providers
        .filterNot { it.intoSet }
        .groupBy { it.typeKey }
    val contributionsByType = providers
        .filter { it.intoSet }
        .groupBy { it.typeKey }
        .mapValues { entry -> entry.value.sortedBy { it.name } }
    val aggregateNameByTypeKey = linkedMapOf<String, String>()
    val aggregateNames = providers.mapTo(mutableSetOf()) { it.name }
    val aggregates = linkedMapOf<String, RawAggregateSet>()

    val resolved = providers.mapNotNull { provider ->
        val dependencyProviders = provider.dependencies.map { dependency ->
            val candidates = typeIndex[dependency.typeKey].orEmpty()
            val aggregate = dependency.toRawAggregateSet(
                contributionsByType = contributionsByType,
                aggregateNameByTypeKey = aggregateNameByTypeKey,
                aggregateNames = aggregateNames,
            )
            when (candidates.size) {
                1 -> {
                    if (aggregate != null) {
                        logger.serviceError(
                            "Both an exact @Service binding and @IntoSet contributions found for dependency " +
                                "`${dependency.name}: ${dependency.typeKey}`. Dependency chain: " +
                                "${provider.address(moduleFqn)} -> ${dependency.name}:${dependency.typeKey}. " +
                                "Exact binding: ${candidates.single().address(moduleFqn)}. " +
                                "Aggregate binding: ${aggregate.name}.",
                            provider.origin,
                        )
                        return@mapNotNull null
                    }
                    candidates.single().name
                }
                0 -> {
                    if (aggregate != null) {
                        aggregates.putIfAbsent(aggregate.typeKey, aggregate)
                        return@map aggregate.name
                    }
                    logger.serviceError(
                        missingServiceBindingMessage(
                            moduleFqn = moduleFqn,
                            providerName = provider.name,
                            providerSignatureDescriptor = provider.signatureDescriptor(),
                            dependencyName = dependency.name,
                            dependencyTypeKey = dependency.typeKey,
                        ),
                        provider.origin,
                    )
                    return@mapNotNull null
                }
                else -> {
                    logger.serviceError(
                        duplicateServiceBindingMessage(
                            moduleFqn = moduleFqn,
                            providerName = provider.name,
                            providerSignatureDescriptor = provider.signatureDescriptor(),
                            dependencyName = dependency.name,
                            dependencyTypeKey = dependency.typeKey,
                            candidateAddresses = candidates.map { it.address(moduleFqn) },
                        ),
                        provider.origin,
                    )
                    return@mapNotNull null
                }
            }
        }
        provider.toModel(dependencyProviders)
    }
    if (resolved.size != providers.size) {
        return null
    }

    val aggregateModels = aggregates.values.map { aggregate ->
        ServiceProviderModel.AggregateSet(
            elementType = aggregate.elementType,
            elementTypeKey = aggregate.elementTypeKey,
            name = aggregate.name,
            returnType = aggregate.returnType,
            typeKey = aggregate.typeKey,
            dependencyProviders = aggregate.contributors.map { it.name },
            origin = aggregate.origin,
        )
    }
    val allProviders = resolved + aggregateModels

    val duplicateNames = allProviders
        .groupingBy { it.name }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    if (duplicateNames.isNotEmpty()) {
        logger.error(
            "Synthetic module $moduleFqn has duplicate binding names: ${duplicateNames.sorted().joinToString()}.",
        )
        return null
    }

    val ordered = try {
        val order = allProviders
            .associate { it.name to it.dependencyProviders }
            .topologicalSort()
            .reversed()
        val byName = allProviders.associateBy { it.name }
        order.map { byName.getValue(it) }
    } catch (cycle: ModuleCycleException) {
        logger.error(cycle.message ?: "Synthetic @Service graph has a cycle")
        return null
    }

    val packageName = moduleFqn.substringBeforeLast('.')
    val simpleName = moduleFqn.substringAfterLast('.')
    return SyntheticModuleModel(packageName, simpleName, moduleFqn, ordered)
}

private fun collectRawServiceProviders(
    resolver: Resolver,
    logger: KSPLogger,
): List<RawServiceProvider> {
    val symbols = resolver
        .getSymbolsWithAnnotation(WellKnownTypes.SERVICE_ANNOTATION)
        .toList()

    return symbols.mapNotNull { symbol ->
        when (symbol) {
            is KSFunctionDeclaration -> symbol.toRawServiceFunction()
            is KSClassDeclaration -> symbol.toRawServiceClass(logger)
            else -> null
        }
    }
}

private fun RawServiceProvider.signatureDescriptor(): String =
    dependencies.joinToString(prefix = "(", postfix = ")") { it.typeKey }.takeIf { it != "()" }.orEmpty()

private fun RawServiceProvider.address(moduleFqn: String): String =
    serviceProviderAddress(moduleFqn, name, signatureDescriptor())

private fun IndexedBinding.toRawServiceProvider(): RawServiceProvider =
    RawServiceProvider(
        name = name,
        returnType = returnType,
        typeKey = typeKey,
        dependencies = emptyList(),
        intoSet = intoSet,
        toModel = {
            ServiceProviderModel.ExternalFunction(
                packageName = packageName,
                functionName = callableName,
                objectClassName = objectFqn?.let(::classNameFromFqn),
                name = name,
                returnType = returnType,
                typeKey = typeKey,
                intoSet = intoSet,
            )
        },
        origin = null,
    )

private fun KSFunctionDeclaration.toRawServiceFunction(): RawServiceProvider? {
    if (getVisibility() != Visibility.PUBLIC) {
        return null
    }
    val returnType = returnType?.resolve() ?: return null
    val bindingType = explicitBindingType() ?: returnType
    val dependencies = parameters.toServiceDependencies()
    val name = simpleName.asString()
    val intoSet = hasAnnotation(WellKnownTypes.INTO_SET_ANNOTATION)
    return RawServiceProvider(
        name = name,
        returnType = bindingType.toTypeName(),
        typeKey = bindingType.typeKey(),
        dependencies = dependencies,
        intoSet = intoSet,
        toModel = { dependencyProviders ->
            ServiceProviderModel.Function(
                declaration = this,
                name = name,
                returnType = bindingType.toTypeName(),
                typeKey = bindingType.typeKey(),
                dependencies = dependencies,
                dependencyProviders = dependencyProviders,
                intoSet = intoSet,
            )
        },
        origin = this,
    )
}

private fun KSClassDeclaration.toRawServiceClass(logger: KSPLogger): RawServiceProvider? {
    if (classKind != ClassKind.CLASS || getVisibility() != Visibility.PUBLIC) {
        return null
    }
    if (Modifier.ABSTRACT in modifiers || Modifier.SEALED in modifiers) {
        return null
    }
    val constructor = primaryConstructor ?: return null
    val functions = getDeclaredFunctions().toList()
    val startupHooks = functions.filter { it.hasAnnotation(STARTUP_ANNOTATION) }
    val shutdownHooks = functions.filter { it.hasAnnotation(SHUTDOWN_ANNOTATION) }
    (startupHooks + shutdownHooks).forEach { hook ->
        if (!hook.isLifecycleHookShapeValid()) {
            logger.error(
                "@Startup/@Shutdown ${qualifiedName?.asString()}.${hook.simpleName.asString()} must be a " +
                    "non-private `suspend` function with no value parameters returning Unit.",
                hook,
            )
            return null
        }
    }
    val bindingType = explicitBindingType() ?: asStarProjectedType()
    val dependencies = constructor.parameters.toServiceDependencies()
    val name = simpleName.asString().replaceFirstChar(Char::lowercaseChar)
    return RawServiceProvider(
        name = name,
        returnType = bindingType.toTypeName(),
        typeKey = bindingType.typeKey(),
        dependencies = dependencies,
        intoSet = false,
        toModel = { dependencyProviders ->
            ServiceProviderModel.Constructor(
                declaration = this,
                startupHooks = startupHooks,
                shutdownHooks = shutdownHooks,
                name = name,
                returnType = bindingType.toTypeName(),
                typeKey = bindingType.typeKey(),
                dependencies = dependencies,
                dependencyProviders = dependencyProviders,
            )
        },
        origin = this,
    )
}

private fun List<KSValueParameter>.toServiceDependencies(): List<ServiceDependency> =
    mapIndexed { index, parameter ->
        val type = parameter.type.resolve()
        val setElementType = type.setElementType()
        ServiceDependency(
            name = parameter.name?.asString() ?: "dependency$index",
            type = type.toTypeName(),
            typeKey = type.typeKey(),
            setElementType = setElementType?.toTypeName(),
            setElementTypeKey = setElementType?.typeKey(),
        )
    }

private fun ServiceDependency.toRawAggregateSet(
    contributionsByType: Map<String, List<RawServiceProvider>>,
    aggregateNameByTypeKey: MutableMap<String, String>,
    aggregateNames: MutableSet<String>,
): RawAggregateSet? {
    val elementType = setElementType ?: return null
    val elementTypeKey = setElementTypeKey ?: return null
    val contributors = contributionsByType[elementTypeKey].orEmpty()
    if (contributors.isEmpty()) {
        return null
    }
    return RawAggregateSet(
        name = aggregateNameByTypeKey.getOrPut(typeKey) {
            uniqueAggregateName(elementTypeKey, aggregateNames)
        },
        returnType = SET.parameterizedBy(elementType),
        typeKey = typeKey,
        elementType = elementType,
        elementTypeKey = elementTypeKey,
        contributors = contributors,
        origin = contributors.first().origin,
    )
}

private fun uniqueAggregateName(
    elementTypeKey: String,
    usedNames: MutableSet<String>,
): String {
    val simpleName = elementTypeKey
        .substringBefore('<')
        .substringAfterLast('.')
        .filter { it.isLetterOrDigit() || it == '_' }
        .ifBlank { "element" }
    val baseName = simpleName.replaceFirstChar(Char::lowercaseChar) + "Set"
    var candidate = baseName
    var suffix = 2
    while (!usedNames.add(candidate)) {
        candidate = "$baseName${suffix++}"
    }
    return candidate
}

private fun KSAnnotated.explicitBindingType(): KSType? {
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

private fun classNameFromFqn(fqn: String): ClassName =
    ClassName(fqn.substringBeforeLast('.'), fqn.substringAfterLast('.'))

private fun KSType.typeKey(): String =
    toTypeName().toString()

private fun KSType.setElementType(): KSType? {
    val typeFqn = declaration.qualifiedName?.asString()
    if (typeFqn != "kotlin.collections.Set") {
        return null
    }
    return arguments.singleOrNull()?.type?.resolve()
}

private const val STARTUP_ANNOTATION = "io.heapy.komok.tech.runtime.Startup"
private const val SHUTDOWN_ANNOTATION = "io.heapy.komok.tech.runtime.Shutdown"

private fun KSPLogger.serviceError(
    message: String,
    origin: KSAnnotated?,
) {
    if (origin == null) {
        error(message)
    } else {
        error(message, origin)
    }
}
