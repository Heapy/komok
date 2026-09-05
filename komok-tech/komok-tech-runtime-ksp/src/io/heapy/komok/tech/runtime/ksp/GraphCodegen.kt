package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Per-root generated code: the interned module DAG, the `modules` accessor container, the
 * `${M}Graph` handle (direct root providers + `realizeAll` + `fork` + `start`/`stop`) and
 * `create${M}Graph`.
 * Generated for every `@Module` (any module can be a graph root); names are root-scoped
 * so there are no collisions.
 *
 * @param reachable root module and its transitive dependencies, in deps-first order.
 */
internal fun generateGraphFile(
    root: ModuleModel,
    reachable: List<ModuleModel>,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): FileSpec {
    val dagClassName = ClassName(root.packageName, "${root.simpleName}Dag")
    val modulesClassName = ClassName(root.packageName, "${root.simpleName}GraphModules")
    val graphClassName = ClassName(root.packageName, "${root.simpleName}Graph")
    val builderClassName = ClassName(root.packageName, "${root.simpleName}GraphBuilder")
    val forkScopeClassName = ClassName(root.packageName, "${root.simpleName}GraphForkScope")
    val buildLifecycleName = "build${root.simpleName}Lifecycle"

    return FileSpec.builder(root.packageName, "${root.simpleName}Graph")
        .addType(generateDag(dagClassName, reachable, models, syntheticModule))
        .addType(generateGraphModules(modulesClassName, dagClassName, reachable, syntheticModule))
        .addType(generateGraph(root, graphClassName, dagClassName, modulesClassName, forkScopeClassName, buildLifecycleName, reachable, models, syntheticModule))
        .addType(generateGraphBuilder(builderClassName))
        .addType(generateRootForkScope(forkScopeClassName, dagClassName, reachable, syntheticModule))
        .addFunction(generateCreateFunction(root, graphClassName, dagClassName, builderClassName))
        .addFunction(generateBuildLifecycle(buildLifecycleName, dagClassName, reachable, syntheticModule))
        .build()
}

private fun generateRootForkScope(
    forkScopeClassName: ClassName,
    dagClassName: ClassName,
    reachable: List<ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): TypeSpec {
    val builder = TypeSpec.classBuilder(forkScopeClassName)
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

    reachable.forEach { module ->
        val moduleForkScope = ClassName(module.packageName, "${module.simpleName}ForkScope")
        val accessor = FunSpec.builder(module.propertyName)
            .addParameter(
                "configure",
                LambdaTypeName.get(receiver = moduleForkScope, returnType = UNIT),
            )
        if (module.dependencies.isNotEmpty() || syntheticModule != null) {
            accessor.addStatement("val scope = %T(overrides, dag.%N)", moduleForkScope, module.propertyName)
        } else {
            accessor.addStatement("val scope = %T(overrides)", moduleForkScope)
        }
        builder.addFunction(accessor.addStatement("scope.configure()").build())
    }
    if (syntheticModule != null) {
        builder.addFunction(
            FunSpec.builder(syntheticModule.propertyName)
                .addParameter(
                    "configure",
                    LambdaTypeName.get(receiver = syntheticModule.forkScopeClassName, returnType = UNIT),
                )
                .addStatement(
                    "val scope = %T(overrides, dag.%N)",
                    syntheticModule.forkScopeClassName,
                    syntheticModule.propertyName,
                )
                .addStatement("scope.configure()")
                .build(),
        )
    }

    return builder.build()
}

private fun generateDag(
    dagClassName: ClassName,
    reachable: List<ModuleModel>,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): TypeSpec {
    val builder = TypeSpec.classBuilder(dagClassName)
        .addModifiers(KModifier.INTERNAL)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("rt", WellKnownTypes.komokRuntime)
                .build(),
        )

    if (syntheticModule != null) {
        builder.addProperty(
            PropertySpec.builder(syntheticModule.propertyName, syntheticModule.komokClassName)
                .initializer("%T(rt)", syntheticModule.komokClassName)
                .build(),
        )
    }

    reachable.forEach { module ->
        val argNames = buildList {
            add("rt")
            module.dependencies.forEach { add(models.getValue(it).propertyName) }
            if (syntheticModule != null) {
                add(syntheticModule.propertyName)
            }
        }
        val format = "%T(" + argNames.joinToString(", ") { "%N" } + ")"
        val args = buildList {
            add(module.komokClassName)
            addAll(argNames)
        }.toTypedArray()
        builder.addProperty(
            PropertySpec.builder(module.propertyName, module.komokClassName)
                .initializer(format, *args)
                .build(),
        )
    }

    return builder.build()
}

private fun generateGraphModules(
    modulesClassName: ClassName,
    dagClassName: ClassName,
    reachable: List<ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): TypeSpec {
    val builder = TypeSpec.classBuilder(modulesClassName)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter("handle", WellKnownTypes.realmHandle)
                .addParameter("dag", dagClassName)
                .build(),
        )

    reachable.forEach { module ->
        builder.addProperty(
            PropertySpec.builder(module.propertyName, module.graphApiClassName)
                .initializer("%T(handle, dag.%N)", module.graphApiClassName, module.propertyName)
                .build(),
        )
    }
    if (syntheticModule != null) {
        builder.addProperty(
            PropertySpec.builder(syntheticModule.propertyName, syntheticModule.graphApiClassName)
                .initializer("%T(handle, dag.%N)", syntheticModule.graphApiClassName, syntheticModule.propertyName)
                .build(),
        )
    }

    return builder.build()
}

private fun generateGraph(
    root: ModuleModel,
    graphClassName: ClassName,
    dagClassName: ClassName,
    modulesClassName: ClassName,
    forkScopeClassName: ClassName,
    buildLifecycleName: String,
    reachable: List<ModuleModel>,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
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
            // Root graph owns the runtime lifecycle; a fork is an overlay that runs no
            // @Startup/@Shutdown hooks and releases only its own resources on stop().
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
                .initializer("%L(handle, dag)", buildLifecycleName)
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
            // The single graceful teardown. Root: run @Shutdown hooks, then release the
            // whole runtime (root realm resources + scope). Fork: release only this
            // overlay's resources — hooks are a root-level concern.
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

    root.providers.forEach { provider ->
        val name = provider.simpleName.asString()
        builder.addFunction(
            FunSpec.builder(name)
                .addModifiers(KModifier.SUSPEND)
                .returns(provider.returnType!!.toTypeName())
                .addStatement("return handle.read·{ dag.%N.%N() }", root.propertyName, name)
                .build(),
        )
    }
    moduleSetAggregates(root, models, syntheticModule).forEach { aggregate ->
        builder.addFunction(
            FunSpec.builder(aggregate.name)
                .addModifiers(KModifier.SUSPEND)
                .returns(aggregate.returnType)
                .addStatement("return handle.read·{ dag.%N.%N() }", root.propertyName, aggregate.name)
                .build(),
        )
    }

    val realizeAll = FunSpec.builder("realizeAll")
        .addModifiers(KModifier.SUSPEND)
        .addKdoc(
            "Realizes every static provider binding, deps-first. The policy-B default is " +
                "realize-then-fork: call this before [fork] so all non-cone bindings are " +
                "completed and shared by reference instead of being recomputed in the child.",
        )
    reachable.forEach { module ->
        module.providers.forEach { provider ->
            realizeAll.addStatement(
                "%M(modules.%N.%N())",
                WellKnownTypes.realized,
                module.propertyName,
                provider.simpleName.asString(),
            )
        }
        moduleSetAggregates(module, models, syntheticModule).forEach { aggregate ->
            realizeAll.addStatement(
                "%M(modules.%N.%N())",
                WellKnownTypes.realized,
                module.propertyName,
                aggregate.name,
            )
        }
    }
    if (syntheticModule != null) {
        syntheticModule.providers.forEach { provider ->
            realizeAll.addStatement(
                "%M(modules.%N.%N())",
                WellKnownTypes.realized,
                syntheticModule.propertyName,
                provider.name,
            )
        }
    }
    builder.addFunction(realizeAll.build())

    builder.addFunction(
        FunSpec.builder("fork")
            .addParameter(
                "configure",
                LambdaTypeName.get(receiver = forkScopeClassName, returnType = UNIT),
            )
            .returns(graphClassName)
            .addStatement(
                "val overrides = mutableMapOf<%T, %T>()",
                WellKnownTypes.bindingId,
                WellKnownTypes.bindingOverride.parameterizedBy(STAR),
            )
            .addStatement("%T(overrides, dag).configure()", forkScopeClassName)
            .addStatement("return %T(handle.fork(overrides), dag, isRoot = false)", graphClassName)
            .build(),
    )

    builder.addFunction(
        FunSpec.builder("graphPayload")
            .returns(WellKnownTypes.graphPayload)
            .addStatement(
                "return %T(modules = %L, edges = handle.runtime.%M())",
                WellKnownTypes.graphPayload,
                moduleDescriptorsCode(reachable, models, syntheticModule),
                WellKnownTypes.edgeDescriptors,
            )
            .build(),
    )

    return builder.build()
}

private fun moduleDescriptorsCode(
    reachable: List<ModuleModel>,
    models: Map<KSClassDeclaration, ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): CodeBlock {
    val code = CodeBlock.builder().add("listOf(")
    var index = 0
    reachable.forEach { module ->
        if (index++ > 0) code.add(", ")
        val dependencyFqns = module.dependencies.map { models.getValue(it).fqn }
        val bindings = module.providers.map { "${module.fqn}.${it.simpleName.asString()}" } +
            moduleSetAggregates(module, models, syntheticModule).map { "${module.fqn}.${it.name}" }
        code.add("%T(", WellKnownTypes.moduleDescriptor)
        code.add("%S, ", module.fqn)
        code.add(
            "listOf(${dependencyFqns.joinToString(", ") { "%S" }}), ",
            *dependencyFqns.toTypedArray(),
        )
        code.add(
            "listOf(${bindings.joinToString(", ") { "%S" }})",
            *bindings.toTypedArray(),
        )
        code.add(")")
    }
    if (syntheticModule != null) {
        if (index > 0) code.add(", ")
        val bindings = syntheticModule.providers.map { "${syntheticModule.fqn}.${it.name}" }
        code.add("%T(", WellKnownTypes.moduleDescriptor)
        code.add("%S, ", syntheticModule.fqn)
        code.add("emptyList(), ")
        code.add("listOf(${bindings.joinToString(", ") { "%S" }})", *bindings.toTypedArray())
        code.add(")")
    }
    return code.add(")").build()
}

private fun generateGraphBuilder(builderClassName: ClassName): TypeSpec =
    TypeSpec.classBuilder(builderClassName)
        .addProperty(
            PropertySpec.builder("dispatcher", WellKnownTypes.coroutineDispatcher)
                .mutable()
                .initializer("%T.IO", WellKnownTypes.dispatchers)
                .build(),
        )
        .addProperty(
            // Always-present elements of the framework-owned realization context — the
            // bootstrap installs its root logging context element here.
            PropertySpec.builder("rootContext", WellKnownTypes.coroutineContext)
                .mutable()
                .initializer("%T", WellKnownTypes.emptyCoroutineContext)
                .build(),
        )
        .build()

private fun generateCreateFunction(
    root: ModuleModel,
    graphClassName: ClassName,
    dagClassName: ClassName,
    builderClassName: ClassName,
): FunSpec {
    val configureType = LambdaTypeName.get(receiver = builderClassName, returnType = UNIT)
    return FunSpec.builder("create${root.simpleName}Graph")
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

private fun generateBuildLifecycle(
    buildLifecycleName: String,
    dagClassName: ClassName,
    reachable: List<ModuleModel>,
    syntheticModule: SyntheticModuleModel?,
): FunSpec {
    val builder = FunSpec.builder(buildLifecycleName)
        .addModifiers(KModifier.PRIVATE)
        .addParameter("handle", WellKnownTypes.realmHandle)
        .addParameter("dag", dagClassName)
        .returns(WellKnownTypes.lifecycle)
        .addStatement("val registry = %T()", WellKnownTypes.lifecycleRegistry)

    // Register hooks in deps-first module order (and declaration order within a module),
    // so the registry is already in startup order.
    reachable.forEach { module ->
        module.startupHooks.forEach { hook ->
            builder.addStatement(
                "registry.register(%S, %T.STARTUP)·{ handle.read·{ dag.%N.%N() } }",
                module.fqn,
                WellKnownTypes.lifecyclePhase,
                module.propertyName,
                hook.simpleName.asString(),
            )
        }
        module.shutdownHooks.forEach { hook ->
            builder.addStatement(
                "registry.register(%S, %T.SHUTDOWN)·{ handle.read·{ dag.%N.%N() } }",
                module.fqn,
                WellKnownTypes.lifecyclePhase,
                module.propertyName,
                hook.simpleName.asString(),
            )
        }
    }
    if (syntheticModule != null) {
        syntheticModule.providers.forEach { provider ->
            if (provider is ServiceProviderModel.Constructor) {
                val moduleId = "${syntheticModule.fqn}.${provider.name}"
                provider.startupHooks.forEach { hook ->
                    builder.addStatement(
                        "registry.register(%S, %T.STARTUP)·{ handle.read·{ dag.%N.%N().%N() } }",
                        moduleId,
                        WellKnownTypes.lifecyclePhase,
                        syntheticModule.propertyName,
                        provider.name,
                        hook.simpleName.asString(),
                    )
                }
                provider.shutdownHooks.forEach { hook ->
                    builder.addStatement(
                        "registry.register(%S, %T.SHUTDOWN)·{ handle.read·{ dag.%N.%N().%N() } }",
                        moduleId,
                        WellKnownTypes.lifecyclePhase,
                        syntheticModule.propertyName,
                        provider.name,
                        hook.simpleName.asString(),
                    )
                }
            }
        }
    }

    return builder
        .addStatement("return %T(registry)", WellKnownTypes.lifecycle)
        .build()
}
