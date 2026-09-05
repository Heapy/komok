package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ContextParameter
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Per-module generated code: the `${M}_Komok` subclass, the `${M}Keys` address object,
 * and the `${M}GraphApi` contextless read handle. Generated once per `@Module`.
 */
internal fun generateModuleFile(
    model: ModuleModel,
    models: Map<KSClassDeclaration, ModuleModel>,
    wrappedTypeFqns: Set<String>,
    syntheticModule: SyntheticModuleModel?,
): FileSpec {
    val aggregates = moduleSetAggregates(model, models, syntheticModule)
    return FileSpec.builder(model.packageName, "${model.simpleName}Komok")
        .addType(generateKomokClass(model, models, wrappedTypeFqns, aggregates, syntheticModule))
        .addType(generateKeysObject(model, aggregates))
        .addType(generateGraphApi(model, aggregates))
        .addType(generateProviderScope(model, models, syntheticModule))
        .addType(generateForkScope(model, wrappedTypeFqns, aggregates, syntheticModule))
        .build()
}

/**
 * The `${T}_KomokDecorated` class name for a provider whose return type is a wrapped
 * (decoratable) type, or `null` when the produced value is not decorated.
 */
private fun decoratorClassName(
    provider: KSFunctionDeclaration,
    wrappedTypeFqns: Set<String>,
): ClassName? {
    val returnDeclaration = provider.returnType?.resolve()?.declaration as? KSClassDeclaration
        ?: return null
    val returnFqn = returnDeclaration.qualifiedName?.asString() ?: return null
    if (returnFqn !in wrappedTypeFqns) return null
    return ClassName(
        returnDeclaration.packageName.asString(),
        "${returnDeclaration.simpleName.asString()}_KomokDecorated",
    )
}

/** `${M}ProviderScope`: typed receiver exposing the module's dependency instances so a
 *  replacement body can call their providers virtually (recording edges, fork-aware). */
private fun generateProviderScope(
    model: ModuleModel,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): TypeSpec {
    val constructor = FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL)
    if (model.dependencies.isNotEmpty() || syntheticModule != null) {
        constructor.addParameter("module", model.komokClassName)
    }

    val builder = TypeSpec.classBuilder("${model.simpleName}ProviderScope")
        .primaryConstructor(constructor.build())

    model.dependencies.forEach { dependency ->
        val dependencyModel = models.getValue(dependency)
        builder.addProperty(
            PropertySpec.builder(dependencyModel.propertyName, dependencyModel.komokClassName)
                .initializer("module.%N", dependencyModel.propertyName)
                .build(),
        )
    }
    if (syntheticModule != null) {
        builder.addProperty(
            PropertySpec.builder(syntheticModule.propertyName, syntheticModule.komokClassName)
                .initializer("module.%N", syntheticModule.propertyName)
                .build(),
        )
    }

    return builder.build()
}

/** `${M}ForkScope`: `replaceY(value)` (pins a value, strips production decorators — the
 *  terminal override for mocks/spies) and `replaceYWith { }` (replaces the raw provider
 *  body; the result is re-wrapped in the production `*_KomokDecorated` decorator, so
 *  `@Log`/`@Metric`/`@Retry` survive a body replacement). */
@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateForkScope(
    model: ModuleModel,
    wrappedTypeFqns: Set<String>,
    aggregates: List<ModuleSetAggregate>,
    syntheticModule: SyntheticModuleModel?,
): TypeSpec {
    val providerScopeClassName = ClassName(model.packageName, "${model.simpleName}ProviderScope")
    val providerOverride = WellKnownTypes.bindingOverride.nestedClass("Provider")
    val valueOverride = WellKnownTypes.bindingOverride.nestedClass("Value")
    val hasDependencies = model.dependencies.isNotEmpty() || syntheticModule != null
    val providerScopeArgument = if (hasDependencies) "module" else ""

    val constructor = FunSpec.constructorBuilder()
        .addParameter("overrides", WellKnownTypes.mutableOverridesMap)
    if (hasDependencies) {
        constructor.addParameter("module", model.komokClassName)
    }

    val builder = TypeSpec.classBuilder("${model.simpleName}ForkScope")
        .primaryConstructor(constructor.build())
        .addProperty(
            PropertySpec.builder("overrides", WellKnownTypes.mutableOverridesMap, KModifier.PRIVATE)
                .initializer("overrides")
                .build(),
        )
    if (hasDependencies) {
        builder.addProperty(
            PropertySpec.builder("module", model.komokClassName, KModifier.PRIVATE)
                .initializer("module")
                .build(),
        )
    }

    model.providers.forEach { provider ->
        val name = provider.simpleName.asString()
        val capitalized = name.replaceFirstChar(Char::uppercaseChar)
        val returnType = provider.providerReturnType()

        builder.addFunction(
            FunSpec.builder("replace$capitalized")
                .addParameter("value", returnType)
                .addStatement(
                    "overrides[%T.%N]·= %T(value)",
                    model.keysClassName,
                    name,
                    valueOverride,
                )
                .build(),
        )

        val bodyType = LambdaTypeName.get(
            receiver = providerScopeClassName,
            parameters = emptyList(),
            returnType = returnType,
            contextParameters = listOf(WellKnownTypes.komokContext),
        ).copy(suspending = true)

        val decorator = decoratorClassName(provider, wrappedTypeFqns)
        val replaceWith = FunSpec.builder("replace${capitalized}With")
            .addParameter("body", bodyType)
        if (decorator == null) {
            replaceWith.addStatement(
                "overrides[%T.%N]·= %T<%T>·{ with(%T($providerScopeArgument))·{ body() } }",
                model.keysClassName,
                name,
                providerOverride,
                returnType,
                providerScopeClassName,
            )
        } else {
            // The replacement supplies the raw value; production decoration is re-applied.
            replaceWith.addStatement(
                "overrides[%T.%N]·= %T<%T>·{ %T(with(%T($providerScopeArgument))·{ body() }) }",
                model.keysClassName,
                name,
                providerOverride,
                returnType,
                decorator,
                providerScopeClassName,
            )
        }
        builder.addFunction(replaceWith.build())
    }
    aggregates.forEach { aggregate ->
        val capitalized = aggregate.name.replaceFirstChar(Char::uppercaseChar)

        builder.addFunction(
            FunSpec.builder("replace$capitalized")
                .addParameter("value", aggregate.returnType)
                .addStatement(
                    "overrides[%T.%N]·= %T(value)",
                    model.keysClassName,
                    aggregate.name,
                    valueOverride,
                )
                .build(),
        )

        val bodyType = LambdaTypeName.get(
            receiver = providerScopeClassName,
            parameters = emptyList(),
            returnType = aggregate.returnType,
            contextParameters = listOf(WellKnownTypes.komokContext),
        ).copy(suspending = true)
        builder.addFunction(
            FunSpec.builder("replace${capitalized}With")
                .addParameter("body", bodyType)
                .addStatement(
                    "overrides[%T.%N]·= %T<%T>·{ with(%T($providerScopeArgument))·{ body() } }",
                    model.keysClassName,
                    aggregate.name,
                    providerOverride,
                    aggregate.returnType,
                    providerScopeClassName,
                )
                .build(),
        )
    }

    return builder.build()
}

private fun generateKomokClass(
    model: ModuleModel,
    models: Map<KSClassDeclaration, ModuleModel>,
    wrappedTypeFqns: Set<String>,
    aggregates: List<ModuleSetAggregate>,
    syntheticModule: SyntheticModuleModel?,
): TypeSpec {
    val isInterfaceModule = model.declaration.classKind == ClassKind.INTERFACE
    val constructor = FunSpec.constructorBuilder()
        .addParameter("rt", WellKnownTypes.komokRuntime)

    val classBuilder = TypeSpec.classBuilder(model.komokClassName)
        .addProperty(
            PropertySpec.builder("rt", WellKnownTypes.komokRuntime, KModifier.PRIVATE)
                .initializer("rt")
                .build(),
        )
    if (isInterfaceModule) {
        classBuilder.addSuperinterface(model.baseClassName)
    } else {
        classBuilder.superclass(model.baseClassName)
    }

    model.dependencies.forEach { dependency ->
        val dependencyModel = models.getValue(dependency)
        constructor.addParameter(dependencyModel.propertyName, dependencyModel.komokClassName)
        // expose the dependency as a property so the generated ProviderScope can reach it.
        classBuilder.addProperty(
            PropertySpec.builder(
                dependencyModel.propertyName,
                dependencyModel.komokClassName,
                KModifier.INTERNAL,
            ).initializer(dependencyModel.propertyName).build(),
        )
        if (!isInterfaceModule) {
            classBuilder.addSuperclassConstructorParameter("%N", dependencyModel.propertyName)
        }
    }
    if (syntheticModule != null) {
        constructor.addParameter(syntheticModule.propertyName, syntheticModule.komokClassName)
        classBuilder.addProperty(
            PropertySpec.builder(
                syntheticModule.propertyName,
                syntheticModule.komokClassName,
                KModifier.INTERNAL,
            ).initializer(syntheticModule.propertyName).build(),
        )
    }

    classBuilder.primaryConstructor(constructor.build())

    model.providers.forEach { provider ->
        classBuilder.addFunction(generateOverride(model, provider, wrappedTypeFqns))
        if (provider.parameters.isNotEmpty()) {
            classBuilder.addFunction(generateParamWiredProvider(model, provider, models, wrappedTypeFqns, aggregates, syntheticModule))
        }
    }
    aggregates.forEach { aggregate ->
        classBuilder.addFunction(generateAggregateProvider(model, aggregate))
    }

    return classBuilder.build()
}

@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateOverride(
    model: ModuleModel,
    provider: KSFunctionDeclaration,
    wrappedTypeFqns: Set<String>,
): FunSpec {
    val name = provider.simpleName.asString()
    val decorator = decoratorClassName(provider, wrappedTypeFqns)
    val arguments = provider.parameters.joinToString(", ") { it.name!!.asString() }
    val rawCall = rawProviderCall(model, provider, arguments)
    val produced = producedValue(decorator, rawCall)
    val builder = FunSpec.builder(name)
        .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
        .contextParameters(listOf(ContextParameter("_", WellKnownTypes.komokContext)))
        .returns(provider.providerReturnType())
    provider.parameters.forEach { parameter ->
        builder.addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
    }
    return builder
        .addStatement(
            "return %N.binding(%T.%N)·{ %L }",
            "rt",
            model.keysClassName,
            name,
            produced,
        )
        .build()
}

@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateParamWiredProvider(
    model: ModuleModel,
    provider: KSFunctionDeclaration,
    models: Map<KSClassDeclaration, ModuleModel>,
    wrappedTypeFqns: Set<String>,
    aggregates: List<ModuleSetAggregate>,
    syntheticModule: SyntheticModuleModel?,
): FunSpec {
    val name = provider.simpleName.asString()
    val decorator = decoratorClassName(provider, wrappedTypeFqns)
    val arguments = providerDependencyCalls(model, provider, models, aggregates, syntheticModule).joinToString(", ")
    val rawCall = rawProviderCall(model, provider, arguments)
    val produced = producedValue(decorator, rawCall)
    return FunSpec.builder(name)
        .addModifiers(KModifier.SUSPEND)
        .contextParameters(listOf(ContextParameter("_", WellKnownTypes.komokContext)))
        .returns(provider.providerReturnType())
        .addStatement(
            "return %N.binding(%T.%N)·{ %L }",
            "rt",
            model.keysClassName,
            name,
            produced,
        )
        .build()
}

@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateAggregateProvider(
    model: ModuleModel,
    aggregate: ModuleSetAggregate,
): FunSpec {
    val args = aggregate.contributors.joinToString(", ") { it.call() }
    return FunSpec.builder(aggregate.name)
        .addModifiers(KModifier.SUSPEND)
        .contextParameters(listOf(ContextParameter("_", WellKnownTypes.komokContext)))
        .returns(aggregate.returnType)
        .addStatement(
            "return %N.binding(%T.%N)·{ linkedSetOf(%L) }",
            "rt",
            model.keysClassName,
            aggregate.name,
            args,
        )
        .build()
}

private fun rawProviderCall(
    model: ModuleModel,
    provider: KSFunctionDeclaration,
    arguments: String,
): CodeBlock =
    if (model.declaration.classKind == ClassKind.INTERFACE) {
        CodeBlock.of("super<%T>.%N(%L)", model.baseClassName, provider.simpleName.asString(), arguments)
    } else {
        CodeBlock.of("super.%N(%L)", provider.simpleName.asString(), arguments)
    }

private fun producedValue(
    decorator: ClassName?,
    rawCall: CodeBlock,
): CodeBlock =
    if (decorator != null) {
        // Decorate the raw value once, at realization, inside the binding boundary.
        CodeBlock.of("%T(%L)", decorator, rawCall)
    } else {
        rawCall
    }

private fun providerDependencyCalls(
    model: ModuleModel,
    provider: KSFunctionDeclaration,
    models: Map<KSClassDeclaration, ModuleModel>,
    aggregates: List<ModuleSetAggregate>,
    syntheticModule: SyntheticModuleModel?,
): List<String> {
    val candidates = moduleProviderCandidates(model, models, syntheticModule)
        .filterNot { it.intoSet }
        .groupBy { it.typeKey }
    val aggregatesByType = aggregates.associateBy { it.typeKey }

    return provider.parameters.map { parameter ->
        val typeKey = parameter.type.resolve().toTypeName().toString()
        val matches = candidates[typeKey].orEmpty()
        val aggregate = aggregatesByType[typeKey]
        require(matches.isEmpty() || aggregate == null) {
            "Both an exact provider and @IntoSet contributions found for parameter " +
                "${parameter.name?.asString()} of ${model.fqn}.${provider.simpleName.asString()}."
        }
        if (aggregate != null) {
            return@map "${aggregate.name}()"
        }
        require(matches.size == 1) {
            "Expected exactly one provider for parameter ${parameter.name?.asString()} of " +
                "${model.fqn}.${provider.simpleName.asString()}, found ${matches.size}."
        }
        matches.single().call()
    }
}

private fun generateKeysObject(
    model: ModuleModel,
    aggregates: List<ModuleSetAggregate>,
): TypeSpec {
    val builder = TypeSpec.objectBuilder(model.keysClassName)
    model.providers.forEach { provider ->
        val name = provider.simpleName.asString()
        val descriptor = provider.providerSignatureDescriptor()
        builder.addProperty(
            PropertySpec.builder(name, WellKnownTypes.bindingId)
                .initializer(
                    "%T(%S, %T(%S, %S))",
                    WellKnownTypes.bindingId,
                    model.fqn,
                    WellKnownTypes.providerSignature,
                    name,
                    descriptor,
                )
                .build(),
        )
    }
    aggregates.forEach { aggregate ->
        builder.addProperty(
            PropertySpec.builder(aggregate.name, WellKnownTypes.bindingId)
                .initializer(
                    "%T(%S, %T(%S, %S))",
                    WellKnownTypes.bindingId,
                    model.fqn,
                    WellKnownTypes.providerSignature,
                    aggregate.name,
                    aggregate.signatureDescriptor,
                )
                .build(),
        )
    }
    return builder.build()
}

private fun generateGraphApi(
    model: ModuleModel,
    aggregates: List<ModuleSetAggregate>,
): TypeSpec {
    val builder = TypeSpec.classBuilder(model.graphApiClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("handle", WellKnownTypes.realmHandle)
                .addParameter("module", model.komokClassName)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("handle", WellKnownTypes.realmHandle, KModifier.PRIVATE)
                .initializer("handle")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("module", model.komokClassName, KModifier.PRIVATE)
                .initializer("module")
                .build(),
        )

    model.providers.forEach { provider ->
        val name = provider.simpleName.asString()
        builder.addFunction(
            FunSpec.builder(name)
                .addModifiers(KModifier.SUSPEND)
                .returns(provider.providerReturnType())
                .addStatement("return handle.read·{ module.%N() }", name)
                .build(),
        )
    }
    aggregates.forEach { aggregate ->
        builder.addFunction(
            FunSpec.builder(aggregate.name)
                .addModifiers(KModifier.SUSPEND)
                .returns(aggregate.returnType)
                .addStatement("return handle.read·{ module.%N() }", aggregate.name)
                .build(),
        )
    }

    return builder.build()
}

private fun KSFunctionDeclaration.providerReturnType(): TypeName =
    returnType!!.toTypeName()
