package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class GraphIrGeneration(private val pluginContext: IrPluginContext, private val services: List<ServiceBinding>, private val decorators: DecoratorIrGeneration) : IrElementTransformerVoidWithContext() {
    private fun runtimeClass(name: String) = pluginContext.finderForSource(currentFile)
        .findClass(ClassId(KomokNames.runtimePackage, Name.identifier(name)))!!.owner

    private fun runtimeFunction(name: String) = pluginContext.finderForSource(currentFile)
        .findFunctions(CallableId(KomokNames.runtimePackage, Name.identifier(name))).single()

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        val key = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? BindingPropertyKey
            ?: return super.visitPropertyNew(declaration)
        val field = requireNotNull(declaration.backingField)
        val builder = DeclarationIrBuilder(pluginContext, field.symbol)
        field.initializer = builder.irExprBody(with(builder) {
            irCallConstructor(runtimeClass("Binding").constructors.single().symbol,
                listOf((field.type as IrSimpleType).arguments.single().typeOrNull!!)).apply {
                type = field.type
                arguments[0] = irCallConstructor(runtimeClass("BindingId").constructors.single().symbol, emptyList()).apply {
                    arguments[0] = irString(key.owner)
                    arguments[1] = irCallConstructor(runtimeClass("ProviderSignature").constructors.single().symbol, emptyList()).apply {
                        arguments[0] = irString(key.provider)
                        arguments[1] = irString(declaration.parentAsClass.parentAsClass.functions.single { it.name.asString() == key.provider }.providerDescriptor())
                    }
                }
            }
        })
        return super.visitPropertyNew(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression = try {
        lowerCall(expression)
    } catch (error: KomokCompilationError) {
        pluginContext.diagnosticReporter.at(expression, currentFile)
            .report(org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSUPPORTED, error.message!!)
        expression
    }

    private fun lowerCall(expression: IrCall): IrExpression {
        expression.transformChildren(this, null)
        val name = (expression.symbol.owner.resolveFakeOverride() ?: expression.symbol.owner).fqNameWhenAvailable?.asString() ?: return expression
        val builder = DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol,
            expression.startOffset, expression.endOffset)
        return when (name) {
            "io.heapy.komok.tech.runtime.binding" -> with(builder) {
                val reference = expression.arguments[0] as? IrFunctionReference
                    ?: komokError("binding requires a service function reference.")
                val service = services.single { it.target.symbol == reference.symbol }
                irCallConstructor(runtimeClass("Binding").constructors.single().symbol, listOf(expression.typeArguments[0]!!)).apply {
                    type = expression.type
                    arguments[0] = irCallConstructor(runtimeClass("BindingId").constructors.single().symbol, emptyList()).apply {
                        arguments[0] = irString(service.owner)
                        arguments[1] = irCallConstructor(runtimeClass("ProviderSignature").constructors.single().symbol, emptyList()).apply {
                            arguments[0] = irString(service.name)
                            arguments[1] = irString(service.descriptor)
                        }
                    }
                }
            }
            "io.heapy.komok.tech.runtime.GraphScope.provider" -> with(builder) {
                val requestedType = expression.typeArguments[0]!!
                irCall(runtimeClass("GraphScope").functions.single { it.name.asString() == "providerByType" }).apply {
                    type = expression.type
                    typeArguments[0] = requestedType
                    arguments[0] = expression.arguments[0]
                    arguments[1] = expression.arguments[1]
                    arguments[2] = irString(requestedType.typeKey())
                }
            }
            "io.heapy.komok.tech.runtime.graph" -> buildGraph(expression, builder, false)
            "io.heapy.komok.tech.runtime.services" -> buildGraph(expression, builder, true)
            "io.heapy.komok.tech.runtime.GraphScope.module" -> with(builder) {
                val resultType = requireNotNull(expression.typeArguments[0])
                val target = runtimeClass("GraphScope").functions.single { it.name.asString() == "moduleByName" }
                irCall(target.symbol).apply {
                    type = resultType
                    typeArguments[0] = resultType
                    arguments[0] = expression.arguments[0]
                    arguments[1] = irString(resultType.typeKey())
                }
            }
            "io.heapy.komok.tech.runtime.GraphScope.binding", "io.heapy.komok.tech.runtime.KomokGraph.resolve" -> with(builder) {
                val requestedType = requireNotNull(expression.typeArguments[0])
                val resolve = name.endsWith(".resolve")
                val target = runtimeClass(if (resolve) "KomokGraph" else "GraphScope").functions
                    .single { it.name.asString() == if (resolve) "resolveType" else "bindingByType" }
                irCall(target.symbol).apply {
                    type = expression.type
                    typeArguments[0] = requestedType
                    arguments[0] = expression.arguments[0]
                    arguments[1] = irString(requestedType.typeKey())
                }
            }
            else -> expression
        }
    }

    private fun buildGraph(call: IrCall, builder: DeclarationIrBuilder, servicesOnly: Boolean): IrExpression {
        val rootType = if (servicesOnly) pluginContext.irBuiltIns.unitType else requireNotNull(call.typeArguments[0])
        val root = requireNotNull(rootType.classOrNull).owner
        komokRequire(servicesOnly || root.hasAnnotation(KomokNames.module.asSingleFqName())) { "graph<T>() requires an @Module type: ${rootType.typeKey()}" }
        val modules = linkedMapOf<String, IrClass>()
        val visiting = linkedSetOf<String>()
        fun visit(module: IrClass) {
            val name = module.fqNameWhenAvailable!!.asString()
            if (name in modules) return
            komokRequire(visiting.add(name)) { "Komok module cycle: ${visiting.joinToString(" -> ")} -> $name" }
            komokRequire(module.kind == ClassKind.INTERFACE || module.primaryConstructor != null) { "Module $name requires a primary constructor." }
            module.primaryConstructor?.parameters.orEmpty().filter { it.kind == IrParameterKind.Regular }.forEach { parameter ->
                val dependency = requireNotNull(parameter.type.classOrNull).owner
                komokRequire(dependency.hasAnnotation(KomokNames.module.asSingleFqName())) {
                    "Module constructor parameter ${parameter.name} must reference another @Module."
                }
                visit(dependency)
            }
            visiting.remove(name)
            modules[name] = module
        }
        if (!servicesOnly) visit(root)
        val bindings = modules.values.flatMap { module ->
            module.functions.filter { it.isKomokProvider() }.map { provider ->
                GraphBinding(module.fqNameWhenAvailable!!.asString(), provider.name.asString(), provider.returnType,
                    provider, module = module, intoSet = provider.hasAnnotation(intoSetAnnotation))
            }.toList()
        } + services.map { GraphBinding(it.owner, it.name, it.providedType, it.target, objectOwner = it.objectOwner, intoSet = it.intoSet) }
        val orderedBindings = validateBindingDependencies(bindings)
        val definitionClass = runtimeClass("GraphDefinition")
        val factory = pluginContext.irFactory.buildFun {
            name = SpecialNames.ANONYMOUS
            returnType = rootType
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply { parent = currentDeclarationParent!! }
        val definition = factory.addValueParameter("definition", definitionClass.defaultType)
        val factoryBuilder = DeclarationIrBuilder(pluginContext, factory.symbol)
        factory.body = factoryBuilder.irBlockBody {
            val instances = linkedMapOf<String, IrVariable>()
            val registerModule = definitionClass.functions.single { it.name.asString() == "registerModule" }
            val registerProvider = definitionClass.functions.single { it.name.asString() == "registerProvider" }
            val registerHook = definitionClass.functions.single { it.name.asString() == "registerHook" }
            modules.forEach { (key, value) ->
                val name = key
                val module = value
                val constructor = if (module.kind == ClassKind.INTERFACE) {
                    val implementation = pluginContext.irFactory.buildClass {
                        this.name = Name.identifier("KomokImplementation_${module.name}")
                        visibility = DescriptorVisibilities.LOCAL
                    }.apply {
                        parent = factory
                        superTypes = listOf(module.defaultType)
                        createThisReceiverParameter()
                    }
                    val generated = implementation.addConstructor { isPrimary = true }
                    generated.body = DeclarationIrBuilder(pluginContext, generated.symbol).irBlockBody {
                        +irDelegatingConstructorCall(pluginContext.irBuiltIns.anyClass.owner.constructors.single())
                    }
                    +implementation
                    generated
                } else module.primaryConstructor!!
                val dependencies = constructor.parameters.filter { it.kind == IrParameterKind.Regular }
                val instance = irTemporary(irCallConstructor(constructor.symbol, emptyList()).apply {
                    dependencies.forEach { parameter ->
                        arguments[parameter.indexInParameters] = irGet(instances.getValue(parameter.type.typeKey()))
                    }
                }, module.name.asString())
                instances[name] = instance
                val providers = module.functions.filter { it.isKomokProvider() }.toList()
                +irCall(registerModule).apply {
                    arguments[0] = irGet(definition)
                    arguments[1] = irString(name)
                    arguments[2] = irGet(instance)
                    arguments[3] = stringList(dependencies.map { it.type.typeKey() }, this@irBlockBody)
                    arguments[4] = stringList(providers.map { "$name.${it.name}" }, this@irBlockBody)
                }
                module.functions.filter {
                    it.hasAnnotation(KomokNames.startup.asSingleFqName()) ||
                        it.hasAnnotation(KomokNames.shutdown.asSingleFqName())
                }.forEach { hook ->
                    +irCall(registerHook).apply {
                        arguments[0] = irGet(definition)
                        arguments[1] = irString(name)
                        arguments[2] = irBoolean(hook.hasAnnotation(KomokNames.startup.asSingleFqName()))
                        arguments[3] = providerLambda(factory, definition, hook, instance)
                    }
                }
            }
            bindings.forEach { binding ->
                +irCall(registerProvider).apply {
                    arguments[0] = irGet(definition)
                    arguments[1] = irString(binding.owner)
                    arguments[2] = irString(binding.name)
                    arguments[3] = irString(binding.descriptor)
                    arguments[4] = irString(binding.type.typeKey())
                    arguments[5] = providerLambda(factory, definition, binding.target,
                        binding.module?.let { instances.getValue(it.fqNameWhenAvailable!!.asString()) }, binding.objectOwner)
                    arguments[6] = irBoolean(binding.module != null)
                    arguments[7] = irBoolean(!binding.intoSet)
                    arguments[8] = decorators.factory(binding.type, factory, currentFile, erased = true)
                }
            }
            val registerSet = definitionClass.functions.single { it.name.asString() == "registerSet" }
            bindings.filter { it.intoSet }.groupBy { it.type.typeKey() }.forEach { (key, value) ->
                +irCall(registerSet).apply {
                    arguments[0] = irGet(definition)
                    arguments[1] = irString("komok.sets")
                    arguments[2] = irString("set<$key>")
                    arguments[3] = irString("kotlin.collections.Set<$key>")
                    arguments[4] = stringList(value.map { it.address }, this@irBlockBody)
                }
            }
            orderedBindings.filter { it.target is IrConstructor }.forEach { binding ->
                binding.target.parentAsClass.functions.filter {
                    it.hasAnnotation(KomokNames.startup.asSingleFqName()) || it.hasAnnotation(KomokNames.shutdown.asSingleFqName())
                }.forEach { hook ->
                    +irCall(registerHook).apply {
                        arguments[0] = irGet(definition)
                        arguments[1] = irString(binding.address)
                        arguments[2] = irBoolean(hook.hasAnnotation(KomokNames.startup.asSingleFqName()))
                        arguments[3] = providerLambda(factory, definition, hook, serviceReceiver = binding.type)
                    }
                }
            }
            +irReturn(if (servicesOnly) irGetObjectValue(rootType, pluginContext.irBuiltIns.unitClass)
                else irGet(instances.getValue(rootType.typeKey())))
        }
        return with(builder) {
            irCall(runtimeFunction("compiledGraph")).apply {
                type = call.type
                typeArguments[0] = rootType
                arguments[0] = call.arguments[0]
                arguments[1] = IrFunctionExpressionImpl(call.startOffset, call.endOffset,
                    pluginContext.irBuiltIns.functionN(1).typeWith(definitionClass.defaultType, rootType),
                    factory, IrStatementOrigin.LAMBDA)
            }
        }
    }

    private fun providerLambda(
        parent: IrFunction, definition: IrValueParameter, provider: IrFunction,
        instance: IrVariable? = null, objectOwner: IrClass? = null, serviceReceiver: IrType? = null,
    ): IrExpression {
        val contextType = runtimeClass("KomokContext").defaultType
        val lambda = pluginContext.irFactory.buildFun {
            name = SpecialNames.ANONYMOUS
            returnType = provider.returnType
            isSuspend = true
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply { this.parent = parent }
        val parameter = lambda.addValueParameter("pluginContext", contextType)
        val builder = DeclarationIrBuilder(pluginContext, lambda.symbol)
        lambda.body = builder.irBlockBody {
            val targetCall = if (provider is IrConstructor) irCallConstructor(provider.symbol, emptyList())
                else irCall((provider as IrSimpleFunction).symbol)
            +irReturn(targetCall.apply {
                provider.parameters.forEach { p ->
                    arguments[p.indexInParameters] = when (p.kind) {
                        IrParameterKind.DispatchReceiver -> when {
                            instance != null -> irGet(instance)
                            objectOwner != null -> irGetObjectValue(objectOwner.defaultType, objectOwner.symbol)
                            serviceReceiver != null -> irAs(resolveValue(definition, parameter, serviceReceiver, this@irBlockBody), p.type)
                            else -> komokError("No receiver for ${provider.name}")
                        }
                        IrParameterKind.Context -> irGet(parameter)
                        IrParameterKind.Regular -> resolveValue(definition, parameter, p.type, this@irBlockBody)
                        else -> komokError("Extension provider is unsupported: ${provider.name}")
                    }
                }
            })
        }
        return IrFunctionExpressionImpl(provider.startOffset, provider.endOffset,
            pluginContext.irBuiltIns.suspendFunctionN(1).typeWith(contextType, provider.returnType), lambda, IrStatementOrigin.LAMBDA)
    }

    private fun resolveValue(definition: IrValueParameter, contextParameter: IrValueParameter, type: IrType, builder: IrBuilderWithScope): IrExpression = with(builder) {
        val resolve = runtimeClass("GraphDefinition").functions.single { it.name.asString() == "resolveValue" }
        irCall(resolve.symbol).apply {
            this.type = type
            typeArguments[0] = type
            arguments[0] = irGet(definition)
            arguments[1] = irGet(contextParameter)
            arguments[2] = irString(type.typeKey())
        }
    }

    private fun stringList(values: List<String>, builder: IrBuilderWithScope): IrExpression = with(builder) {
        val listOf = pluginContext.finderForBuiltins().findFunctions(CallableId(FqName("kotlin.collections"), Name.identifier("listOf")))
            .single { it.owner.parameters.singleOrNull()?.varargElementType != null }
        irCall(listOf).apply {
            type = pluginContext.irBuiltIns.listClass.typeWith(pluginContext.irBuiltIns.stringType)
            typeArguments[0] = pluginContext.irBuiltIns.stringType
            arguments[0] = irVararg(pluginContext.irBuiltIns.stringType, values.map { irString(it) })
        }
    }
}

internal fun IrSimpleFunction.isKomokProvider(): Boolean =
    isSuspend && !returnType.isUnit() && visibility == DescriptorVisibilities.PUBLIC &&
        (parentAsClass.kind == ClassKind.INTERFACE || parameters.none { it.kind == IrParameterKind.Regular }) &&
        !hasAnnotation(KomokNames.startup.asSingleFqName()) && !hasAnnotation(KomokNames.shutdown.asSingleFqName())

internal fun IrType.typeKey(): String {
    val simple = this as IrSimpleType
    val name = simple.classFqName?.asString() ?: komokError("Komok requires a concrete type: $this")
    val arguments = simple.arguments.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") {
        it.typeOrNull?.typeKey() ?: "*"
    }.orEmpty()
    return name + arguments + if (simple.isNullable()) "?" else ""
}
