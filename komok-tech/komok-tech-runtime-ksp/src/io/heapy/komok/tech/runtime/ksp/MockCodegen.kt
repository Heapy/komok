package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ContextParameter
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

private val copyOnWriteArrayList = ClassName("java.util.concurrent", "CopyOnWriteArrayList")

internal fun generateMockFile(type: WrappedType): FileSpec {
    val mockName = ClassName(type.packageName, "${type.simpleName}Mock")
    val factoryName = ClassName(type.packageName, "${type.simpleName}MockFactory")
    return FileSpec.builder(type.packageName, "${type.simpleName}Mock")
        .addType(generateMockClass(mockName, type))
        .addType(generateMockFactoryObject(factoryName, mockName, type))
        .addFunction(generateMockFactory(mockName, type))
        .addFunction(generateSpyFactory(mockName, type))
        .addFunction(generateTypedMockFactory(factoryName, mockName))
        .addFunction(generateTypedSpyFactory(factoryName, mockName, type))
        .build()
}

private fun generateMockClass(
    mockName: ClassName,
    type: WrappedType,
): TypeSpec {
    val serviceType = type.declaration.toClassName()
    val builder = TypeSpec.classBuilder(mockName)
        .addSuperinterface(serviceType)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("delegate", serviceType.copy(nullable = true))
                        .defaultValue("null")
                        .build(),
                )
                .build(),
        )
        .addProperty(
            PropertySpec.builder("delegate", serviceType.copy(nullable = true), KModifier.PRIVATE)
                .initializer("delegate")
                .build(),
        )

    type.properties.forEach { property ->
        builder.addProperty(propertyCallLogProperty(property))
        builder.addProperty(propertyStubProperty(property))
        builder.addFunction(propertyStubFunction(property))
        builder.addFunction(propertyVerifyFunction(property))
        builder.addProperty(mockProperty(type, property))
    }
    type.methods.forEach { method ->
        builder.addProperty(callLogProperty(method))
        builder.addProperty(stubProperty(method))
        builder.addFunction(stubFunction(method))
        builder.addFunction(verifyFunction(method))
        builder.addFunction(mockMethod(type, method))
    }

    return builder.build()
}

private fun generateMockFactoryObject(
    factoryName: ClassName,
    mockName: ClassName,
    type: WrappedType,
): TypeSpec =
    TypeSpec.objectBuilder(factoryName)
        .addFunction(
            FunSpec.builder("mock")
                .returns(mockName)
                .addStatement("return %T()", mockName)
                .build(),
        )
        .addFunction(
            FunSpec.builder("spy")
                .addParameter("real", type.declaration.toClassName())
                .returns(mockName)
                .addStatement("return %T(real)", mockName)
                .build(),
        )
        .build()

private fun generateMockFactory(
    mockName: ClassName,
    type: WrappedType,
): FunSpec =
    FunSpec.builder("${type.simpleName.replaceFirstChar(Char::lowercaseChar)}Mock")
        .returns(mockName)
        .addStatement("return %T()", mockName)
        .build()

private fun generateSpyFactory(
    mockName: ClassName,
    type: WrappedType,
): FunSpec =
    FunSpec.builder("${type.simpleName.replaceFirstChar(Char::lowercaseChar)}Spy")
        .addParameter("real", type.declaration.toClassName())
        .returns(mockName)
        .addStatement("return %T(real)", mockName)
        .build()

private fun generateTypedMockFactory(
    factoryName: ClassName,
    mockName: ClassName,
): FunSpec =
    FunSpec.builder("mock")
        .addParameter("factory", factoryName)
        .returns(mockName)
        .addStatement("return factory.mock()")
        .build()

private fun generateTypedSpyFactory(
    factoryName: ClassName,
    mockName: ClassName,
    type: WrappedType,
): FunSpec =
    FunSpec.builder("spy")
        .addParameter("factory", factoryName)
        .addParameter("real", type.declaration.toClassName())
        .returns(mockName)
        .addStatement("return factory.spy(real)")
        .build()

private fun callLogProperty(method: WrappedMethod): PropertySpec {
    val callsType = MUTABLE_LIST.parameterizedBy(LIST.parameterizedBy(ANY.copy(nullable = true)))
    return PropertySpec.builder("${method.name}Calls", callsType, KModifier.PRIVATE)
        .initializer("%T()", copyOnWriteArrayList)
        .build()
}

private fun stubProperty(method: WrappedMethod): PropertySpec =
    PropertySpec.builder(
        "${method.name}Stub",
        stubLambdaType(method).copy(nullable = true),
        KModifier.PRIVATE,
    )
        .mutable()
        .initializer("null")
        .build()

private fun stubLambdaType(method: WrappedMethod): LambdaTypeName {
    val declaration = method.declaration
    val lambda = LambdaTypeName.get(
        parameters = declaration.parameters.map { parameter ->
            ParameterSpec.builder(parameter.name!!.asString(), parameter.type.toTypeName())
                .build()
        },
        returnType = method.returnType(),
    )
    return if (Modifier.SUSPEND in declaration.modifiers) {
        lambda.copy(suspending = true)
    } else {
        lambda
    }
}

private fun stubFunction(method: WrappedMethod): FunSpec =
    FunSpec.builder("every${method.name.replaceFirstChar(Char::uppercaseChar)}")
        .addParameter("body", stubLambdaType(method))
        .addStatement("%N = body", "${method.name}Stub")
        .build()

private fun propertyCallLogProperty(property: WrappedProperty): PropertySpec {
    val callsType = MUTABLE_LIST.parameterizedBy(LIST.parameterizedBy(ANY.copy(nullable = true)))
    return PropertySpec.builder("${property.name}Calls", callsType, KModifier.PRIVATE)
        .initializer("%T()", copyOnWriteArrayList)
        .build()
}

private fun propertyStubProperty(property: WrappedProperty): PropertySpec =
    PropertySpec.builder(
        "${property.name}Stub",
        LambdaTypeName.get(returnType = property.returnType()).copy(nullable = true),
        KModifier.PRIVATE,
    )
        .mutable()
        .initializer("null")
        .build()

private fun propertyStubFunction(property: WrappedProperty): FunSpec =
    FunSpec.builder("every${property.name.replaceFirstChar(Char::uppercaseChar)}")
        .addParameter("body", LambdaTypeName.get(returnType = property.returnType()))
        .addStatement("%N = body", "${property.name}Stub")
        .build()

private fun propertyVerifyFunction(property: WrappedProperty): FunSpec =
    FunSpec.builder("verify${property.name.replaceFirstChar(Char::uppercaseChar)}")
        .addParameter(
            ParameterSpec.builder("times", Int::class)
                .defaultValue("1")
                .build(),
        )
        .addStatement("val actual = %N.size", "${property.name}Calls")
        .addStatement("check(actual == times)·{ %P }", "Expected ${property.name} to be called \$times times, but was \$actual")
        .build()

private fun verifyFunction(method: WrappedMethod): FunSpec {
    val builder = FunSpec.builder("verify${method.name.replaceFirstChar(Char::uppercaseChar)}")
    method.declaration.parameters.forEach { parameter ->
        builder.addParameter(parameter.name!!.asString(), parameter.type.toTypeName())
    }
    builder.addParameter(
        ParameterSpec.builder("times", Int::class)
            .defaultValue("1")
            .build(),
    )
    val args = method.arguments()
    val countExpression = if (args.isBlank()) {
        "${method.name}Calls.size"
    } else {
        "${method.name}Calls.count·{ it == listOf($args) }"
    }
    return builder
        .addStatement("val actual = %L", countExpression)
        .addStatement("check(actual == times)·{ %P }", "Expected ${method.name} to be called \$times times, but was \$actual")
        .build()
}

private fun mockProperty(
    type: WrappedType,
    property: WrappedProperty,
): PropertySpec =
    PropertySpec.builder(property.name, property.returnType(), KModifier.OVERRIDE)
        .getter(
            FunSpec.getterBuilder()
                .addCode(mockPropertyBody(type, property))
                .build(),
        )
        .build()

private fun mockPropertyBody(
    type: WrappedType,
    property: WrappedProperty,
): CodeBlock =
    CodeBlock.builder()
        .addStatement("%N.add(emptyList())", "${property.name}Calls")
        .addStatement("val stub = %N", "${property.name}Stub")
        .beginControlFlow("if (stub != null)")
        .addStatement("return stub()")
        .endControlFlow()
        .addStatement("val real = delegate")
        .beginControlFlow("if (real != null)")
        .addStatement("return real.%N", property.name)
        .endControlFlow()
        .addStatement("error(%S)", "No stub configured for ${type.fqn}.${property.name}")
        .build()

@OptIn(ExperimentalKotlinPoetApi::class)
private fun mockMethod(
    type: WrappedType,
    method: WrappedMethod,
): FunSpec {
    val declaration = method.declaration
    val builder = FunSpec.builder(method.name)
        .addModifiers(KModifier.OVERRIDE)

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
    builder.addCode(mockMethodBody(type, method))
    return builder.build()
}

private fun mockMethodBody(
    type: WrappedType,
    method: WrappedMethod,
): CodeBlock {
    val args = method.arguments()
    val listArgs = if (args.isBlank()) "" else args
    return CodeBlock.builder()
        .addStatement("%N.add(listOf(%L))", "${method.name}Calls", listArgs)
        .addStatement("val stub = %N", "${method.name}Stub")
        .beginControlFlow("if (stub != null)")
        .addStatement("return stub(%L)", args)
        .endControlFlow()
        .addStatement("val real = delegate")
        .beginControlFlow("if (real != null)")
        .addStatement("return real.%N(%L)", method.name, args)
        .endControlFlow()
        .addStatement("error(%S)", "No stub configured for ${type.fqn}.${method.name}")
        .build()
}

private fun WrappedMethod.arguments(): String =
    declaration.parameters.joinToString(", ") { it.name!!.asString() }

private fun WrappedMethod.returnType(): TypeName =
    declaration.returnType?.toTypeName() ?: UNIT

private fun WrappedProperty.returnType(): TypeName =
    declaration.type.toTypeName()
