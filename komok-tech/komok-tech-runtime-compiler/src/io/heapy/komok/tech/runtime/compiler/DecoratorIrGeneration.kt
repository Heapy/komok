package io.heapy.komok.tech.runtime.compiler

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
import org.jetbrains.kotlin.name.*

/** Generates only the produced-value decorator; the module itself stays final. */
internal class DecoratorIrGeneration(private val pluginContext: IrPluginContext) {
    private val marker = FqName("io.heapy.komok.tech.runtime.CallWrapper")
    private var sequence = 0
    private val implementations = linkedMapOf<String, IrClass>()

    fun emitClasses() {
        implementations.values.forEach { (it.parent as IrFile).declarations.add(it) }
    }

    private data class Wrapper(val annotation: IrAnnotation, val helper: String, val order: Int)

    private fun wrappers(service: IrClass, method: IrSimpleFunction): List<Wrapper> {
        val annotations = (service.annotations + method.annotations).associateBy { it.classSymbol }
        return annotations.values.mapNotNull { annotation ->
            val meta = annotation.classSymbol.owner.getAnnotation(marker) ?: return@mapNotNull null
            Wrapper(annotation, (meta.argument("helper") as IrConst).value as String, (meta.argument("order") as IrConst).value as Int)
        }.sortedBy { it.order }
    }

    /** Null means no decorator is needed. Erased factories are stored for replaceWith. */
    fun factory(type: IrType, parent: IrDeclarationParent, file: IrFile, erased: Boolean = false): IrExpression? {
        val service = type.classOrNull?.owner ?: return null
        val members = service.simpleFunctions().filterNot { it.isMethodOfAny() }.toList()
        if (members.none { wrappers(service, it).isNotEmpty() }) return null
        komokRequire(service.kind == ClassKind.INTERFACE) { "Decorated provider types must be interfaces: ${type.typeKey()}" }
        val factoryType = if (erased) pluginContext.irBuiltIns.anyNType else type
        val factory = pluginContext.irFactory.buildFun {
            name = SpecialNames.ANONYMOUS
            returnType = factoryType
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply { this.parent = parent }
        val value = factory.addValueParameter("value", factoryType)
        fun result(constructor: IrConstructor): IrExpression {
            factory.body = DeclarationIrBuilder(pluginContext, factory.symbol).irBlockBody {
                +irReturn(irCallConstructor(constructor.symbol, emptyList()).apply {
                    arguments[0] = if (erased) irAs(irGet(value), type) else irGet(value)
                })
            }
            return IrFunctionExpressionImpl(-1, -1,
                pluginContext.irBuiltIns.functionN(1).typeWith(factoryType, factoryType), factory, IrStatementOrigin.LAMBDA)
        }
        implementations[type.typeKey()]?.let { return result(it.primaryConstructor!!) }
        val implementation = pluginContext.irFactory.buildClass {
            name = Name.identifier("KomokDecorated_${service.name}_${sequence++}")
            visibility = DescriptorVisibilities.INTERNAL
            modality = Modality.FINAL
        }.apply {
            this.parent = file
            superTypes = listOf(type)
            createThisReceiverParameter()
        }
        val delegate = implementation.addField("delegate", type)
        val constructor = implementation.addConstructor { isPrimary = true }
        val parameter = constructor.addValueParameter("delegate", type)
        constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(pluginContext.irBuiltIns.anyClass.owner.constructors.single())
            +irSetField(irGet(implementation.thisReceiver!!), delegate, irGet(parameter))
        }
        val substitutor = IrTypeSubstitutor(service.typeParameters.map { it.symbol },
            (type as IrSimpleType).arguments.map { requireNotNull(it.typeOrNull) })
        val properties = mutableMapOf<Name, IrProperty>()
        members.forEach { original ->
            val chain = wrappers(service, original)
            komokRequire(chain.isEmpty() || original.isSuspend) { "Decorated methods must be suspend: ${service.name}.${original.name}" }
            komokRequire(original.typeParameters.isEmpty()) { "Generic decorated methods are not supported: ${original.name}" }
            val method = pluginContext.irFactory.buildFun {
                name = original.name
                returnType = substitutor.substitute(original.returnType)
                isSuspend = original.isSuspend
                visibility = DescriptorVisibilities.PUBLIC
                modality = Modality.FINAL
            }.apply {
                this.parent = implementation
                overriddenSymbols = listOf(original.symbol)
                parameters = original.parameters.map { p -> p.copyTo(this, defaultValue = null,
                    type = if (p.kind == IrParameterKind.DispatchReceiver) implementation.defaultType else substitutor.substitute(p.type)) }
            }
            val originalProperty = original.correspondingPropertySymbol?.owner
            if (originalProperty == null) implementation.declarations.add(method) else {
                val property = properties.getOrPut(originalProperty.name) {
                    implementation.addProperty {
                        name = originalProperty.name
                        isVar = originalProperty.isVar
                        visibility = DescriptorVisibilities.PUBLIC
                        modality = Modality.FINAL
                    }.apply { overriddenSymbols = listOf(originalProperty.symbol) }
                }
                method.correspondingPropertySymbol = property.symbol
                if (original == originalProperty.getter) property.getter = method else property.setter = method
            }
            val metaField = if (chain.isEmpty()) null else implementation.addField {
                name = Name.identifier("meta_${implementation.declarations.size}")
                this.type = pluginContext.finderForSource(file).findClass(
                    ClassId.topLevel(FqName("io.heapy.komok.tech.runtime.wrappers.MethodMeta")))!!.defaultType
                isStatic = true
                isFinal = true
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                val initializerBuilder = DeclarationIrBuilder(pluginContext, symbol)
                initializer = initializerBuilder.irExprBody(metadata(service, original, initializerBuilder, file))
            }
            val builder = DeclarationIrBuilder(pluginContext, method.symbol)
            fun body(position: Int, owner: IrFunction, b: IrBuilderWithScope): IrExpression = with(b) {
                if (position == chain.size) return@with irCall(original).apply {
                    this.type = method.returnType
                    original.parameters.forEach { p ->
                        arguments[p.indexInParameters] = if (p.kind == IrParameterKind.DispatchReceiver)
                            irGetField(irGet(method.dispatchReceiverParameter!!), delegate)
                        else irGet(method.parameters[p.indexInParameters])
                    }
                }
                val wrapper = chain[position]
                val helperName = FqName(wrapper.helper)
                val helper = pluginContext.finderForSource(file).findFunctions(CallableId(helperName.parent(), helperName.shortName())).single().owner
                val lambda = pluginContext.irFactory.buildFun {
                    name = SpecialNames.ANONYMOUS
                    returnType = method.returnType
                    isSuspend = true
                    visibility = DescriptorVisibilities.LOCAL
                    origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                }.apply { this.parent = owner }
                lambda.body = DeclarationIrBuilder(pluginContext, lambda.symbol).irBlockBody {
                    +irReturn(body(position + 1, lambda, this))
                }
                irCall(helper).apply {
                    this.type = method.returnType
                    typeArguments[0] = method.returnType
                    helper.parameters.forEach { p ->
                        arguments[p.indexInParameters] = when {
                            p.kind == IrParameterKind.Context -> irGet(method.parameters.single {
                                it.kind == IrParameterKind.Context && it.type.typeKey() == p.type.typeKey()
                            })
                            p.name.asString() == "meta" -> irGetField(null, metaField!!)
                            p.name.asString() == "call" -> IrFunctionExpressionImpl(method.startOffset, method.endOffset,
                                pluginContext.irBuiltIns.suspendFunctionN(0).typeWith(method.returnType), lambda, IrStatementOrigin.LAMBDA)
                            else -> wrapper.annotation.argument(p.name.asString())?.deepCopyWithSymbols(owner)
                        }
                    }
                }
            }
            method.body = builder.irBlockBody { +irReturn(body(0, method, this)) }
        }
        implementations[type.typeKey()] = implementation
        return result(constructor)
    }

    private fun metadata(service: IrClass, method: IrSimpleFunction, builder: IrBuilderWithScope, file: IrFile): IrExpression = with(builder) {
        fun type(name: String) = pluginContext.finderForSource(file).findClass(ClassId.topLevel(FqName("io.heapy.komok.tech.runtime.wrappers.$name")))!!.owner
        val parameter = type("ParameterMeta")
        val values = method.parameters.filter { it.kind == IrParameterKind.Regular }.map { p ->
            irCallConstructor(parameter.constructors.single().symbol, emptyList()).apply {
                arguments[0] = irString(p.name.asString())
                arguments[1] = irString(p.type.typeKey())
            }
        }
        val listOf = pluginContext.finderForBuiltins().findFunctions(CallableId(FqName("kotlin.collections"), Name.identifier("listOf")))
            .single { it.owner.parameters.singleOrNull()?.varargElementType != null }
        irCallConstructor(type("MethodMeta").primaryConstructor!!.symbol, emptyList()).apply {
            arguments[0] = irString(service.fqNameWhenAvailable!!.asString())
            arguments[1] = irString(method.name.asString())
            arguments[2] = irCall(listOf).apply {
                this.type = pluginContext.irBuiltIns.listClass.typeWith(parameter.defaultType)
                typeArguments[0] = parameter.defaultType
                arguments[0] = irVararg(parameter.defaultType, values)
            }
            arguments[3] = irString(method.returnType.typeKey())
            arguments[4] = irBoolean(method.isSuspend)
        }
    }
}
