package io.heapy.komok.tech.runtime.ksp

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ContextParameter
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName

internal fun generateSyntheticServiceFile(
    model: SyntheticModuleModel,
): FileSpec {
    val dagClassName = ClassName(model.packageName, "${model.simpleName}Dag")
    val modulesClassName = ClassName(model.packageName, "${model.simpleName}GraphModules")
    val graphClassName = ClassName(model.packageName, "${model.simpleName}Graph")
    val builderClassName = ClassName(model.packageName, "${model.simpleName}GraphBuilder")
    val graphForkScopeClassName = ClassName(model.packageName, "${model.simpleName}GraphForkScope")

    return FileSpec.builder(model.packageName, "${model.simpleName}Graph")
        .addType(generateSyntheticKomokClass(model))
        .addType(generateSyntheticKeysObject(model))
        .addType(generateSyntheticGraphApi(model))
        .addType(generateSyntheticProviderScope(model))
        .addType(generateSyntheticForkScope(model))
        .addType(generateSyntheticDag(model, dagClassName))
        .addType(generateSyntheticGraphModules(model, modulesClassName, dagClassName))
        .addType(generateSyntheticGraph(model, graphClassName, dagClassName, modulesClassName, graphForkScopeClassName))
        .addType(generateSyntheticGraphBuilder(builderClassName))
        .addType(generateSyntheticGraphForkScope(model, graphForkScopeClassName, dagClassName))
        .addFunction(generateSyntheticCreateFunction(model, graphClassName, dagClassName, builderClassName))
        .addFunction(generateSyntheticBuildLifecycle(model, dagClassName))
        .build()
}

private fun generateSyntheticKomokClass(model: SyntheticModuleModel): TypeSpec {
    val builder = TypeSpec.classBuilder(model.komokClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("rt", WellKnownTypes.komokRuntime)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("rt", WellKnownTypes.komokRuntime, KModifier.PRIVATE)
                .initializer("rt")
                .build(),
        )

    model.providers.forEach { provider ->
        builder.addFunction(generateSyntheticProvider(model, provider))
    }

    return builder.build()
}

@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateSyntheticProvider(
    model: SyntheticModuleModel,
    provider: ServiceProviderModel,
): FunSpec {
    val body = providerBody(provider)
    return FunSpec.builder(provider.name)
        .addModifiers(KModifier.SUSPEND)
        .contextParameters(listOf(ContextParameter("_", WellKnownTypes.komokContext)))
        .returns(provider.returnType)
        .addStatement(
            "return rt.binding(%T.%N)·{ %L }",
            model.keysClassName,
            provider.name,
            body,
        )
        .build()
}

private fun providerBody(provider: ServiceProviderModel): CodeBlock {
    val args = provider.dependencyProviders.joinToString(", ") { "$it()" }
    return when (provider) {
        is ServiceProviderModel.Function ->
            CodeBlock.of("%L.%N(%L)", provider.declaration.packageName.asString(), provider.declaration.simpleName.asString(), args)
        is ServiceProviderModel.Constructor ->
            CodeBlock.of("%T(%L)", provider.declaration.toClassName(), args)
        is ServiceProviderModel.AggregateSet ->
            CodeBlock.of("linkedSetOf(%L)", args)
        is ServiceProviderModel.ExternalFunction ->
            if (provider.objectClassName == null) {
                CodeBlock.of("%L.%N(%L)", provider.packageName, provider.functionName, args)
            } else {
                CodeBlock.of("%T.%N(%L)", provider.objectClassName, provider.functionName, args)
            }
    }
}

private fun generateSyntheticKeysObject(model: SyntheticModuleModel): TypeSpec {
    val builder = TypeSpec.objectBuilder(model.keysClassName)
    model.providers.forEach { provider ->
        builder.addProperty(
            PropertySpec.builder(provider.name, WellKnownTypes.bindingId)
                .initializer(
                    "%T(%S, %T(%S, %S))",
                    WellKnownTypes.bindingId,
                    model.fqn,
                    WellKnownTypes.providerSignature,
                    provider.name,
                    provider.signatureDescriptor,
                )
                .build(),
        )
    }
    return builder.build()
}

private fun generateSyntheticGraphApi(model: SyntheticModuleModel): TypeSpec {
    val builder = TypeSpec.classBuilder(model.graphApiClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
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
        builder.addFunction(
            FunSpec.builder(provider.name)
                .addModifiers(KModifier.SUSPEND)
                .returns(provider.returnType)
                .addStatement("return handle.read·{ module.%N() }", provider.name)
                .build(),
        )
    }

    return builder.build()
}

@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateSyntheticProviderScope(model: SyntheticModuleModel): TypeSpec {
    val builder = TypeSpec.classBuilder(model.providerScopeClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter("module", model.komokClassName)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("module", model.komokClassName, KModifier.PRIVATE)
                .initializer("module")
                .build(),
        )

    model.providers.forEach { provider ->
        builder.addFunction(
            FunSpec.builder(provider.name)
                .addModifiers(KModifier.SUSPEND)
                .contextParameters(listOf(ContextParameter("_", WellKnownTypes.komokContext)))
                .returns(provider.returnType)
                .addStatement("return module.%N()", provider.name)
                .build(),
        )
    }

    return builder.build()
}

@OptIn(ExperimentalKotlinPoetApi::class)
private fun generateSyntheticForkScope(model: SyntheticModuleModel): TypeSpec {
    val providerOverride = WellKnownTypes.bindingOverride.nestedClass("Provider")
    val valueOverride = WellKnownTypes.bindingOverride.nestedClass("Value")
    val builder = TypeSpec.classBuilder(model.forkScopeClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter("overrides", WellKnownTypes.mutableOverridesMap)
                .addParameter("module", model.komokClassName)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("overrides", WellKnownTypes.mutableOverridesMap, KModifier.PRIVATE)
                .initializer("overrides")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("module", model.komokClassName, KModifier.PRIVATE)
                .initializer("module")
                .build(),
        )

    model.providers.forEach { provider ->
        val capitalized = provider.name.replaceFirstChar(Char::uppercaseChar)
        builder.addFunction(
            FunSpec.builder("replace$capitalized")
                .addParameter("value", provider.returnType)
                .addStatement(
                    "overrides[%T.%N]·= %T(value)",
                    model.keysClassName,
                    provider.name,
                    valueOverride,
                )
                .build(),
        )

        val bodyType = LambdaTypeName.get(
            receiver = model.providerScopeClassName,
            returnType = provider.returnType,
            contextParameters = listOf(WellKnownTypes.komokContext),
        ).copy(suspending = true)
        builder.addFunction(
            FunSpec.builder("replace${capitalized}With")
                .addParameter("body", bodyType)
                .addStatement(
                    "overrides[%T.%N]·= %T<%T>·{ with(%T(module))·{ body() } }",
                    model.keysClassName,
                    provider.name,
                    providerOverride,
                    provider.returnType,
                    model.providerScopeClassName,
                )
                .build(),
        )
    }

    return builder.build()
}

private fun generateSyntheticDag(
    model: SyntheticModuleModel,
    dagClassName: ClassName,
): TypeSpec =
    TypeSpec.classBuilder(dagClassName)
        .addModifiers(KModifier.INTERNAL)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("rt", WellKnownTypes.komokRuntime)
                .build(),
        )
        .addProperty(
            PropertySpec.builder(model.propertyName, model.komokClassName)
                .initializer("%T(rt)", model.komokClassName)
                .build(),
        )
        .build()

private fun generateSyntheticGraphModules(
    model: SyntheticModuleModel,
    modulesClassName: ClassName,
    dagClassName: ClassName,
): TypeSpec =
    TypeSpec.classBuilder(modulesClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter("handle", WellKnownTypes.realmHandle)
                .addParameter("dag", dagClassName)
                .build(),
        )
        .addProperty(
            PropertySpec.builder(model.propertyName, model.graphApiClassName)
                .initializer("%T(handle, dag.%N)", model.graphApiClassName, model.propertyName)
                .build(),
        )
        .build()

private fun generateSyntheticGraph(
    model: SyntheticModuleModel,
    graphClassName: ClassName,
    dagClassName: ClassName,
    modulesClassName: ClassName,
    graphForkScopeClassName: ClassName,
): TypeSpec {
    val builder = TypeSpec.classBuilder(graphClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter("handle", WellKnownTypes.realmHandle)
                .addParameter("dag", dagClassName)
                .addParameter("isRoot", BOOLEAN)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("handle", WellKnownTypes.realmHandle, KModifier.PRIVATE)
                .initializer("handle")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("dag", dagClassName, KModifier.PRIVATE)
                .initializer("dag")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("isRoot", BOOLEAN, KModifier.PRIVATE)
                .initializer("isRoot")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("modules", modulesClassName)
                .initializer("%T(handle, dag)", modulesClassName)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("lifecycle", WellKnownTypes.lifecycle, KModifier.PRIVATE)
                .initializer("build${model.simpleName}Lifecycle(handle, dag)")
                .build(),
        )
        .addFunction(
            FunSpec.builder("start")
                .addModifiers(KModifier.SUSPEND)
                .beginControlFlow("if (isRoot)")
                .addStatement("lifecycle.startup()")
                .endControlFlow()
                .build(),
        )
        .addFunction(
            FunSpec.builder("stop")
                .addModifiers(KModifier.SUSPEND)
                .beginControlFlow("if (isRoot)")
                .addStatement("lifecycle.shutdown()")
                .addStatement("handle.runtime.close()")
                .nextControlFlow("else")
                .addStatement("handle.close()")
                .endControlFlow()
                .build(),
        )

    model.providers.forEach { provider ->
        builder.addFunction(
            FunSpec.builder(provider.name)
                .addModifiers(KModifier.SUSPEND)
                .returns(provider.returnType)
                .addStatement("return modules.%N.%N()", model.propertyName, provider.name)
                .build(),
        )
    }

    val realizeAll = FunSpec.builder("realizeAll")
        .addModifiers(KModifier.SUSPEND)
    model.providers.forEach { provider ->
        realizeAll.addStatement("%M(modules.%N.%N())", WellKnownTypes.realized, model.propertyName, provider.name)
    }
    builder.addFunction(realizeAll.build())

    builder.addFunction(
        FunSpec.builder("fork")
            .addParameter(
                "configure",
                LambdaTypeName.get(receiver = graphForkScopeClassName, returnType = UNIT),
            )
            .returns(graphClassName)
            .addStatement(
                "val overrides = mutableMapOf<%T, %T>()",
                WellKnownTypes.bindingId,
                WellKnownTypes.bindingOverride.parameterizedBy(STAR),
            )
            .addStatement("%T(overrides, dag).configure()", graphForkScopeClassName)
            .addStatement("return %T(handle.fork(overrides), dag, isRoot = false)", graphClassName)
            .build(),
    )

    builder.addFunction(
        FunSpec.builder("graphPayload")
            .returns(WellKnownTypes.graphPayload)
            .addStatement(
                "return %T(modules = listOf(%L), edges = handle.runtime.%M())",
                WellKnownTypes.graphPayload,
                syntheticModuleDescriptor(model),
                WellKnownTypes.edgeDescriptors,
            )
            .build(),
    )

    return builder.build()
}

private fun syntheticModuleDescriptor(model: SyntheticModuleModel): CodeBlock {
    val bindings = model.providers.map { "${model.fqn}.${it.name}" }
    return CodeBlock.builder()
        .add("%T(", WellKnownTypes.moduleDescriptor)
        .add("%S, ", model.fqn)
        .add("emptyList(), ")
        .add("listOf(${bindings.joinToString(", ") { "%S" }})", *bindings.toTypedArray())
        .add(")")
        .build()
}

private fun generateSyntheticGraphBuilder(builderClassName: ClassName): TypeSpec =
    TypeSpec.classBuilder(builderClassName)
        .addProperty(
            PropertySpec.builder("dispatcher", WellKnownTypes.coroutineDispatcher)
                .mutable()
                .initializer("%T.IO", WellKnownTypes.dispatchers)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("rootContext", WellKnownTypes.coroutineContext)
                .mutable()
                .initializer("%T", WellKnownTypes.emptyCoroutineContext)
                .build(),
        )
        .build()

private fun generateSyntheticGraphForkScope(
    model: SyntheticModuleModel,
    graphForkScopeClassName: ClassName,
    dagClassName: ClassName,
): TypeSpec =
    TypeSpec.classBuilder(graphForkScopeClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter("overrides", WellKnownTypes.mutableOverridesMap)
                .addParameter("dag", dagClassName)
                .build(),
        )
        .addProperty(
            PropertySpec.builder("overrides", WellKnownTypes.mutableOverridesMap, KModifier.PRIVATE)
                .initializer("overrides")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("dag", dagClassName, KModifier.PRIVATE)
                .initializer("dag")
                .build(),
        )
        .addFunction(
            FunSpec.builder(model.propertyName)
                .addParameter(
                    "configure",
                    LambdaTypeName.get(receiver = model.forkScopeClassName, returnType = UNIT),
                )
                .addStatement("val scope = %T(overrides, dag.%N)", model.forkScopeClassName, model.propertyName)
                .addStatement("scope.configure()")
                .build(),
        )
        .build()

private fun generateSyntheticCreateFunction(
    model: SyntheticModuleModel,
    graphClassName: ClassName,
    dagClassName: ClassName,
    builderClassName: ClassName,
): FunSpec {
    val configureType = LambdaTypeName.get(receiver = builderClassName, returnType = UNIT)
    return FunSpec.builder("create${model.simpleName}Graph")
        .addParameter(
            ParameterSpec.builder("configure", configureType)
                .defaultValue("{}")
                .build(),
        )
        .returns(graphClassName)
        .addStatement("val builder = %T().apply(configure)", builderClassName)
        .addStatement(
            "val rt = %T(dispatcher = builder.dispatcher, rootContext = builder.rootContext)",
            WellKnownTypes.komokRuntime,
        )
        .addStatement("val dag = %T(rt)", dagClassName)
        .addStatement("return %T(handle = rt.rootHandle(), dag = dag, isRoot = true)", graphClassName)
        .build()
}

private fun generateSyntheticBuildLifecycle(
    model: SyntheticModuleModel,
    dagClassName: ClassName,
): FunSpec {
    val builder = FunSpec.builder("build${model.simpleName}Lifecycle")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("handle", WellKnownTypes.realmHandle)
        .addParameter("dag", dagClassName)
        .returns(WellKnownTypes.lifecycle)
        .addStatement("val registry = %T()", WellKnownTypes.lifecycleRegistry)

    model.providers.forEach { provider ->
        if (provider is ServiceProviderModel.Constructor) {
            val moduleId = "${model.fqn}.${provider.name}"
            provider.startupHooks.forEach { hook ->
                builder.addStatement(
                    "registry.register(%S, %T.STARTUP)·{ handle.read·{ dag.%N.%N().%N() } }",
                    moduleId,
                    WellKnownTypes.lifecyclePhase,
                    model.propertyName,
                    provider.name,
                    hook.simpleName.asString(),
                )
            }
            provider.shutdownHooks.forEach { hook ->
                builder.addStatement(
                    "registry.register(%S, %T.SHUTDOWN)·{ handle.read·{ dag.%N.%N().%N() } }",
                    moduleId,
                    WellKnownTypes.lifecyclePhase,
                    model.propertyName,
                    provider.name,
                    hook.simpleName.asString(),
                )
            }
        }
    }

    return builder
        .addStatement("return %T(registry)", WellKnownTypes.lifecycle)
        .build()
}
