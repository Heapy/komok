package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class ModuleSetAggregate(
    val name: String,
    val returnType: TypeName,
    val typeKey: String,
    val elementType: TypeName,
    val elementTypeKey: String,
    val contributors: List<ModuleSetContributor>,
) {
    val signatureDescriptor: String = ""
}

internal class ModuleSetContributor(
    val receiver: String?,
    val address: String,
    val name: String,
    val typeKey: String,
    val intoSet: Boolean,
) {
    fun call(): String =
        if (receiver == null) {
            "$name()"
        } else {
            "$receiver.$name()"
        }
}

internal fun moduleSetAggregates(
    model: ModuleModel,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel? = null,
): List<ModuleSetAggregate> {
    val providerNames = model.providers.mapTo(mutableSetOf()) { it.simpleName.asString() }
    val aggregateNames = providerNames.toMutableSet()
    val aggregateNameByTypeKey = linkedMapOf<String, String>()
    val candidatesByType = moduleProviderCandidates(model, models, syntheticModule)
        .filter { it.intoSet }
        .groupBy { it.typeKey }
        .mapValues { entry ->
            entry.value.sortedBy { it.address }
        }
    val aggregates = linkedMapOf<String, ModuleSetAggregate>()

    model.providers.forEach { provider ->
        provider.parameters.forEach { parameter ->
            val type = parameter.type.resolve()
            val elementType = type.setElementType() ?: return@forEach
            val elementTypeKey = elementType.typeKey()
            val contributors = candidatesByType[elementTypeKey].orEmpty()
            if (contributors.isEmpty()) {
                return@forEach
            }
            val typeKey = type.typeKey()
            aggregates.putIfAbsent(
                typeKey,
                ModuleSetAggregate(
                    name = aggregateNameByTypeKey.getOrPut(typeKey) {
                        uniqueSetAggregateName(elementTypeKey, aggregateNames)
                    },
                    returnType = SET.parameterizedBy(elementType.toTypeName()),
                    typeKey = typeKey,
                    elementType = elementType.toTypeName(),
                    elementTypeKey = elementTypeKey,
                    contributors = contributors,
                ),
            )
        }
    }

    return aggregates.values.toList()
}

internal fun moduleProviderCandidates(
    model: ModuleModel,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel? = null,
): List<ModuleSetContributor> =
    buildList {
        model.providers.forEach { candidate ->
            add(moduleSetContributor(receiver = null, module = model, provider = candidate))
        }
        model.dependencies.forEach { dependency ->
            val dependencyModel = models.getValue(dependency)
            dependencyModel.providers.forEach { candidate ->
                add(moduleSetContributor(receiver = dependencyModel.propertyName, module = dependencyModel, provider = candidate))
            }
        }
        syntheticModule?.providers?.forEach { provider ->
            add(
                ModuleSetContributor(
                    receiver = syntheticModule.propertyName,
                    address = "${syntheticModule.fqn}.${provider.name}",
                    name = provider.name,
                    typeKey = provider.typeKey,
                    intoSet = provider.intoSet,
                ),
            )
        }
    }

private fun moduleSetContributor(
    receiver: String?,
    module: ModuleModel,
    provider: KSFunctionDeclaration,
): ModuleSetContributor {
    val name = provider.simpleName.asString()
    return ModuleSetContributor(
        receiver = receiver,
        address = "${module.fqn}.$name",
        name = name,
        typeKey = provider.providerTypeKey(),
        intoSet = provider.hasAnnotation(WellKnownTypes.INTO_SET_ANNOTATION),
    )
}

internal fun KSFunctionDeclaration.providerTypeKey(): String =
    returnType!!.resolve().toTypeName().toString()

internal fun KSFunctionDeclaration.providerSignatureDescriptor(): String =
    parameters
        .joinToString(prefix = "(", postfix = ")") { parameter ->
            parameter.type.resolve().toTypeName().toString()
        }
        .takeIf { it != "()" }
        .orEmpty()

internal fun KSAnnotated.hasAnnotation(fqn: String): Boolean =
    annotations.any { annotation ->
        annotation.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
    }

private fun uniqueSetAggregateName(
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

private fun KSType.typeKey(): String =
    toTypeName().toString()

private fun KSType.setElementType(): KSType? {
    val typeFqn = declaration.qualifiedName?.asString()
    if (typeFqn != "kotlin.collections.Set") {
        return null
    }
    return arguments.singleOrNull()?.type?.resolve()
}
