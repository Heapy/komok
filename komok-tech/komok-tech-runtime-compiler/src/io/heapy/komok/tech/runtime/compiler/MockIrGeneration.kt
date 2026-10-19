package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class MockIrGeneration(private val pluginContext: IrPluginContext) : IrElementTransformerVoidWithContext() {
    private var sequence = 0

    private fun runtimeClass(name: String): IrClass = pluginContext.finderForSource(currentFile)
        .findClass(ClassId(KomokNames.runtimePackage, Name.identifier(name)))!!.owner

    override fun visitCall(expression: IrCall): IrExpression = try {
        lowerCall(expression)
    } catch (error: KomokCompilationError) {
        pluginContext.diagnosticReporter.at(expression, currentFile)
            .report(org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSUPPORTED, error.message!!)
        expression
    }

    private fun lowerCall(expression: IrCall): IrExpression {
        expression.transformChildren(this, null)
        val name = expression.symbol.owner.fqNameWhenAvailable?.asString() ?: return expression
        val builder = DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol,
            expression.startOffset, expression.endOffset)
        return when (name) {
            "io.heapy.komok.tech.runtime.mock", "io.heapy.komok.tech.runtime.spy" ->
                generateMock(expression, builder, name.endsWith(".spy"))
            "io.heapy.komok.tech.runtime.Mock.every", "io.heapy.komok.tech.runtime.Mock.calls" ->
                selector(expression, builder, name.endsWith(".every"))
            else -> expression
        }
    }

    private fun selector(call: IrCall, builder: DeclarationIrBuilder, stub: Boolean): IrExpression {
        val lambda = (call.arguments[1] as? IrFunctionExpression)?.function
            ?: komokError("Komok method selector must be a lambda containing one call.")
        val statements = (lambda.body as? IrBlockBody)?.statements.orEmpty().filterNot {
            it is IrReturn && it.value is IrGetObjectValue && it.value.type.isUnit()
        }
        val statement = statements.singleOrNull()
        val selected = (if (statement is IrReturn) statement.value else statement) as? IrCall
            ?: komokError("Komok method selector must contain exactly one method or property access.")
        val receiver = selected.arguments.firstOrNull() as? IrGetValue
        komokRequire(lambda.parameters.any { it.symbol == receiver?.symbol }) {
            "Komok selector must call a member on the selector receiver."
        }
        val target = runtimeClass("Mock").functions.single { it.name.asString() == if (stub) "stub" else "recordedCalls" }
        return with(builder) {
            irCall(target.symbol).apply {
                type = call.type
                if (stub) typeArguments[0] = call.typeArguments[0]
                arguments[0] = call.arguments[0]
                arguments[1] = irString(selected.symbol.owner.mockKey())
            }
        }
    }

    private fun generateMock(call: IrCall, builder: DeclarationIrBuilder, spy: Boolean): IrExpression {
        val serviceType = requireNotNull(call.typeArguments[0])
        val service = requireNotNull(serviceType.classOrNull).owner
        komokRequire(service.kind == ClassKind.INTERFACE) { "Komok mocks require an interface: ${serviceType.typeKey()}" }
        komokRequire(service.modality != Modality.SEALED) { "Komok mocks cannot implement sealed interfaces: ${serviceType.typeKey()}" }
        val implementation = pluginContext.irFactory.buildClass {
            name = Name.identifier("KomokMock_${service.name}_${sequence++}")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = currentDeclarationParent!!
            superTypes = listOf(serviceType)
            createThisReceiverParameter()
        }
        val controllerClass = runtimeClass("MockController")
        val controllerField = implementation.addField("controller", controllerClass.defaultType)
        val delegateField = if (spy) implementation.addField("delegate", serviceType) else null
        val constructor = implementation.addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.PUBLIC
        }
        val controllerParameter = constructor.addValueParameter("controller", controllerClass.defaultType)
        val delegateParameter = if (spy) constructor.addValueParameter("delegate", serviceType) else null
        constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(pluginContext.irBuiltIns.anyClass.owner.constructors.single())
            +irSetField(irGet(implementation.thisReceiver!!), controllerField, irGet(controllerParameter))
            if (delegateField != null) +irSetField(irGet(implementation.thisReceiver!!), delegateField, irGet(delegateParameter!!))
        }
        val substitutions = service.typeParameters.map { it.symbol }.zip(
            (serviceType as IrSimpleType).arguments.map { requireNotNull(it.typeOrNull) },
        ).toMap()
        val substitutor = IrTypeSubstitutor(substitutions.keys.toList(), substitutions.values.toList())
        val properties = mutableMapOf<Name, IrProperty>()
        service.simpleFunctions().filterNot { it.isMethodOfAny() }.forEach { original ->
            komokRequire(original.typeParameters.isEmpty()) { "Generic mock methods are not supported: ${original.name}" }
            val method = pluginContext.irFactory.buildFun {
                name = original.name
                returnType = substitutor.substitute(original.returnType)
                isSuspend = original.isSuspend
                visibility = DescriptorVisibilities.PUBLIC
                modality = Modality.FINAL
            }.apply {
                parent = implementation
                overriddenSymbols = listOf(original.symbol)
                parameters = original.parameters.map { parameter ->
                    parameter.copyTo(this, defaultValue = null, type =
                        if (parameter.kind == IrParameterKind.DispatchReceiver) implementation.defaultType
                        else substitutor.substitute(parameter.type))
                }
            }
            val property = original.correspondingPropertySymbol?.owner
            if (property == null) {
                implementation.declarations.add(method)
            } else {
                val generated = properties.getOrPut(property.name) {
                    implementation.addProperty {
                        name = property.name
                        isVar = property.isVar
                        visibility = DescriptorVisibilities.PUBLIC
                        modality = Modality.FINAL
                    }.apply { overriddenSymbols = listOf(property.symbol) }
                }
                method.correspondingPropertySymbol = generated.symbol
                if (original == property.getter) generated.getter = method else generated.setter = method
            }
            val dispatch = method.parameters.single { it.kind == IrParameterKind.DispatchReceiver }
            val methodBuilder = DeclarationIrBuilder(pluginContext, method.symbol)
            method.body = methodBuilder.irBlockBody {
                val invoke = controllerClass.functions.single { it.name.asString() == if (method.isSuspend) "suspendCall" else "call" }
                val invocation = irCall(invoke).apply {
                    arguments[0] = irGetField(irGet(dispatch), controllerField)
                    arguments[1] = irString(original.mockKey())
                    arguments[2] = anyList(method.parameters.filter { it.kind == IrParameterKind.Regular }.map { irGet(it) }, this@irBlockBody)
                    arguments[3] = if (delegateField == null) irNull() else
                        delegateLambda(method, original, dispatch, delegateField)
                }
                +irReturn(irAs(invocation, method.returnType))
            }
        }
        return builder.irBlock(resultType = call.type) {
            +implementation
            val controller = irTemporary(irCallConstructor(controllerClass.constructors.single().symbol, emptyList()))
            val value = irCallConstructor(constructor.symbol, emptyList()).apply {
                arguments[0] = irGet(controller)
                if (spy) arguments[1] = call.arguments[0]
            }
            +irCallConstructor(runtimeClass("Mock").constructors.single().symbol, listOf(serviceType)).apply {
                type = call.type
                arguments[0] = value
                arguments[1] = irGet(controller)
            }
        }
    }

    private fun delegateLambda(method: IrSimpleFunction, original: IrSimpleFunction, dispatch: IrValueParameter, field: IrField): IrExpression {
        val lambda = pluginContext.irFactory.buildFun {
            name = SpecialNames.ANONYMOUS
            returnType = method.returnType
            isSuspend = method.isSuspend
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply { parent = method }
        lambda.body = DeclarationIrBuilder(pluginContext, lambda.symbol).irBlockBody {
            +irReturn(irCall(original).apply {
                type = method.returnType
                original.parameters.forEach { parameter ->
                    arguments[parameter.indexInParameters] = if (parameter.kind == IrParameterKind.DispatchReceiver)
                        irGetField(irGet(dispatch), field)
                    else irGet(method.parameters[parameter.indexInParameters])
                }
            })
        }
        val functionType = (if (method.isSuspend) pluginContext.irBuiltIns.suspendFunctionN(0) else pluginContext.irBuiltIns.functionN(0))
            .typeWith(method.returnType)
        return IrFunctionExpressionImpl(method.startOffset, method.endOffset, functionType, lambda, IrStatementOrigin.LAMBDA)
    }

    private fun anyList(values: List<IrExpression>, builder: IrBuilderWithScope): IrExpression = with(builder) {
        val listOf = pluginContext.finderForBuiltins().findFunctions(CallableId(FqName("kotlin.collections"), Name.identifier("listOf")))
            .single { it.owner.parameters.singleOrNull()?.varargElementType != null }
        irCall(listOf).apply {
            type = pluginContext.irBuiltIns.listClass.typeWith(pluginContext.irBuiltIns.anyNType)
            typeArguments[0] = pluginContext.irBuiltIns.anyNType
            arguments[0] = irVararg(pluginContext.irBuiltIns.anyNType, values)
        }
    }
}

internal fun IrSimpleFunction.mockKey(): String = name.asString() + parameters
    .filter { it.kind == IrParameterKind.Regular }.joinToString(",", "(", ")") { it.type.mockTypeKey() }

private fun IrType.mockTypeKey(): String {
    val type = this as IrSimpleType
    return if (type.classOrNull == null) "#${type.classifier.owner.let { (it as IrTypeParameter).index }}"
        else type.classFqName!!.asString() + type.arguments.takeIf { it.isNotEmpty() }
            ?.joinToString(",", "<", ">") { it.typeOrNull?.mockTypeKey() ?: "*" }.orEmpty() +
            if (type.isNullable()) "?" else ""
}
