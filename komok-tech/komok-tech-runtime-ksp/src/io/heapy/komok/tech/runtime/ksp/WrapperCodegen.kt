package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ContextParameter
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.heapy.komok.tech.runtime.ksp.wrapper.WrappedTypeInfo
import io.heapy.komok.tech.runtime.ksp.wrapper.WrapperSite

/**
 * Generates, for one decoratable service interface, the produced-value decorator
 * `${T}_KomokDecorated(delegate)` plus the static `${T}Meta` holding a [MethodMeta] per
 * wrapped method. The decorator implements the interface, plain-delegates un-annotated
 * methods, and nests the applied wrappers in deterministic chain order (ascending
 * `WrapperPlugin.order`; built-ins: `logCall` -> `metricCall` -> `retryCall` ->
 * delegate). No reflection.
 *
 * Wrapper kinds are not known to this file: each applied wrapper is a discovered
 * `WrapperPlugin` (ServiceLoader SPI) contributing its helper invocation, optional
 * decorator properties, and optional context parameter.
 *
 * **Wrapper context arrives as context parameters, never statically.** A wrapped method
 * declares the contexts its wrappers need (e.g. `@Log` -> `context(_: LoggingContext)`,
 * `@Metric` -> `context(_: MeterRegistry)`), in chain order; the caller provides them at
 * the call site, the same way providers receive `KomokContext`. The generated override
 * reproduces exactly those context parameters, so the interface method must declare them
 * too (KSP cannot see context parameters, so a mismatch surfaces as a compile error in
 * the generated code rather than a KSP diagnostic; the same applies to un-annotated
 * methods, which must NOT declare context parameters).
 */
internal fun generateDecoratorFile(type: WrappedType): FileSpec {
    val metaName = ClassName(type.packageName, "${type.simpleName}Meta")
    val decoratorName = ClassName(type.packageName, "${type.simpleName}_KomokDecorated")
    return FileSpec.builder(type.packageName, "${type.simpleName}_KomokDecorated")
        .addType(generateMetaObject(metaName, type))
        .addType(generateDecorator(decoratorName, metaName, type))
        .build()
}

private fun generateMetaObject(metaName: ClassName, type: WrappedType): TypeSpec {
    val builder = TypeSpec.objectBuilder(metaName).addModifiers(KModifier.INTERNAL)
    type.methods.filter { it.isWrapped }.forEach { method ->
        builder.addProperty(
            PropertySpec.builder(method.name, WellKnownTypes.methodMeta)
                .initializer(methodMetaInitializer(type, method))
                .build(),
        )
    }
    return builder.build()
}

private fun methodMetaInitializer(type: WrappedType, method: WrappedMethod): CodeBlock {
    val parameters = method.declaration.parameters.map { parameter ->
        CodeBlock.of(
            "%T(%S, %S)",
            WellKnownTypes.parameterMeta,
            parameter.name?.asString() ?: "",
            parameter.type.resolve().declaration.qualifiedName?.asString() ?: "?",
        )
    }
    val returnType = method.declaration.returnType?.resolve()?.declaration?.qualifiedName?.asString()
        ?: "kotlin.Unit"
    return CodeBlock.builder()
        .add("%T(\n", WellKnownTypes.methodMeta)
        .indent()
        .add("owner = %S,\n", type.fqn)
        .add("name = %S,\n", method.name)
        .add("parameters = listOf(%L),\n", parameters.joinToCode())
        .add("returnType = %S,\n", returnType)
        .add("isSuspend = %L,\n", Modifier.SUSPEND in method.declaration.modifiers)
        .unindent()
        .add(")")
        .build()
}

private fun generateDecorator(
    decoratorName: ClassName,
    metaName: ClassName,
    type: WrappedType,
): TypeSpec {
    val builder = TypeSpec.classBuilder(decoratorName)
        .addSuperinterface(type.declaration.toClassName())
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("delegate", type.declaration.toClassName())
                .build(),
        )
        .addProperty(
            PropertySpec.builder("delegate", type.declaration.toClassName(), KModifier.PRIVATE)
                .initializer("delegate")
                .build(),
        )

    val typeInfo = WrappedTypeInfo(type.declaration.toClassName(), type.fqn)
    val properties = LinkedHashMap<String, PropertySpec>()
    type.methods
        .flatMap { it.applied }
        .map { it.plugin }
        .distinctBy { it.annotationFqn }
        .flatMap { it.decoratorProperties(typeInfo) }
        .forEach { property -> properties.putIfAbsent(property.name, property) }
    properties.values.forEach { builder.addProperty(it) }

    type.properties.forEach { property ->
        builder.addProperty(decoratorProperty(property))
    }
    type.methods.forEach { method ->
        builder.addFunction(decoratorMethod(metaName, method))
    }

    return builder.build()
}

private fun decoratorProperty(property: WrappedProperty): PropertySpec =
    PropertySpec.builder(property.name, property.declaration.type.toTypeName(), KModifier.OVERRIDE)
        .getter(
            FunSpec.getterBuilder()
                .addStatement("return delegate.%N", property.name)
                .build(),
        )
        .build()

@OptIn(ExperimentalKotlinPoetApi::class)
private fun decoratorMethod(metaName: ClassName, method: WrappedMethod): FunSpec {
    val declaration = method.declaration
    val builder = FunSpec.builder(method.name).addModifiers(KModifier.OVERRIDE)
    // Context parameters are the applied wrappers' requirements in chain order; the
    // interface method must declare the same ones.
    val contextParameters = method.applied
        .mapNotNull { it.plugin.contextParameter }
        .distinct()
        .map { ContextParameter("_", it) }
    if (contextParameters.isNotEmpty()) {
        builder.contextParameters(contextParameters)
    }
    if (Modifier.SUSPEND in declaration.modifiers) {
        builder.addModifiers(KModifier.SUSPEND)
    }
    declaration.parameters.forEach { parameter ->
        builder.addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
    }
    declaration.returnType?.let { builder.returns(it.toTypeName()) }
    builder.addCode(decoratorBody(metaName, method))
    return builder.build()
}

private fun decoratorBody(metaName: ClassName, method: WrappedMethod): CodeBlock {
    val arguments = method.declaration.parameters.joinToString(", ") { it.name!!.asString() }
    if (!method.isWrapped) {
        return CodeBlock.of("return delegate.%N(%L)\n", method.name, arguments)
    }

    val meta = CodeBlock.of("%T.%N", metaName, method.name)
    val code = CodeBlock.builder()
    method.applied.forEachIndexed { index, applied ->
        val call = applied.plugin.helperCall(
            WrapperSite(applied.annotation, method.declaration, meta),
        )
        val prefix = if (index == 0) "return " else ""
        code.beginControlFlow(prefix + "%L", call)
    }
    code.addStatement("delegate.%N(%L)", method.name, arguments)
    repeat(method.applied.size) { code.endControlFlow() }
    return code.build()
}
