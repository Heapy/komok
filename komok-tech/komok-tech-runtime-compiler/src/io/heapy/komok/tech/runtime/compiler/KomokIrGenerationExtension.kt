package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class KomokIrGenerationExtension(private val configuration: CompilerConfiguration) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (moduleFragment.files.isEmpty()) return
        try {
            generateChecked(moduleFragment, pluginContext)
        } catch (error: KomokCompilationError) {
            pluginContext.diagnosticReporter.at(moduleFragment.files.first(), moduleFragment.files.first())
                .report(org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSUPPORTED, error.message!!)
        }
    }

    private fun generateChecked(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val decorators = DecoratorIrGeneration(pluginContext)
        val services = ServiceCatalog(pluginContext, configuration, moduleFragment)
        moduleFragment.transformChildrenVoid(GraphIrGeneration(pluginContext, services.bindings, decorators))
        moduleFragment.transformChildrenVoid(MockIrGeneration(pluginContext))
        services.emitIndex()
        val boundary = pluginContext.finderForSource(moduleFragment.files.first()).findFunctions(
            CallableId(KomokNames.runtimePackage, Name.identifier("komokBinding")),
        ).singleOrNull() ?: return
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                super.visitSimpleFunction(declaration)
                val owner = declaration.parent as? IrClass ?: return declaration
                if (!owner.hasAnnotation(KomokNames.module.asSingleFqName()) ||
                    !declaration.isKomokProvider() ||
                    declaration.visibility != DescriptorVisibilities.PUBLIC ||
                    declaration.hasAnnotation(KomokNames.startup.asSingleFqName()) ||
                    declaration.hasAnnotation(KomokNames.shutdown.asSingleFqName())) return declaration
                val oldBody = declaration.body ?: return declaration
                val contextParameter = declaration.parameters.single { it.kind == IrParameterKind.Context }
                val lambda = pluginContext.irFactory.buildFun {
                    name = SpecialNames.ANONYMOUS
                    returnType = declaration.returnType
                    isSuspend = true
                    visibility = DescriptorVisibilities.LOCAL
                    origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                }.apply { parent = declaration }
                val enteredContext = lambda.addValueParameter("context", contextParameter.type)
                lambda.body = oldBody
                oldBody.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression =
                        if (expression.symbol == contextParameter.symbol)
                            IrGetValueImpl(expression.startOffset, expression.endOffset, enteredContext.symbol)
                        else expression

                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid(this)
                        if (expression.returnTargetSymbol == declaration.symbol) {
                            expression.returnTargetSymbol = lambda.symbol
                        }
                        return expression
                    }
                })
                oldBody.patchDeclarationParents(lambda)
                val functionType = pluginContext.irBuiltIns.suspendFunctionN(1).typeWith(
                    contextParameter.type, declaration.returnType,
                )
                val functionExpression = IrFunctionExpressionImpl(
                    declaration.startOffset, declaration.endOffset, functionType,
                    lambda, IrStatementOrigin.LAMBDA,
                )
                val builder = DeclarationIrBuilder(pluginContext, declaration.symbol)
                declaration.body = builder.irBlockBody {
                    +irReturn(irCall(boundary).apply {
                        type = declaration.returnType
                        typeArguments[0] = declaration.returnType
                        arguments[0] = irGet(contextParameter)
                        arguments[1] = irString(owner.fqNameWhenAvailable!!.asString())
                        arguments[2] = irString(declaration.name.asString())
                        arguments[3] = irString(declaration.providerDescriptor())
                        arguments[4] = functionExpression
                        arguments[5] = decorators.factory(declaration.returnType, declaration, declaration.file)
                    })
                }
                return declaration
            }
        })
        decorators.emitClasses()
    }
}
