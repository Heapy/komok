package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

fun String.builderClassName(): String =
    "${this}Builder"

fun String.toPropertyName(): String =
    replaceFirstChar(Char::lowercase)

fun KSClassDeclaration.toPropertyName(): String =
    simpleName
        .asString()
        .toPropertyName()

fun generateBuilderClass(
    module: KSClassDeclaration,
    moduleDependencies: List<KSClassDeclaration>,
    properties: List<KSPropertyDeclaration>,
): TypeSpec {
    val className = module.simpleName.asString()

    return TypeSpec
        .classBuilder(className.builderClassName())
        .addBuilderClassConstructor(moduleDependencies)
        .addDependentModules(moduleDependencies)
        .addModuleProperties(properties)
        .addModuleBuildFunction(
            module,
            properties,
        )
        .build()
}

private fun TypeSpec.Builder.addBuilderClassConstructor(
    moduleDependencies: List<KSClassDeclaration>,
): TypeSpec.Builder {
    val constructor = FunSpec
        .constructorBuilder()
        .apply {
            moduleDependencies.forEach {
                val constructorParameter = ParameterSpec
                    .builder(
                        it.toPropertyName(),
                        ClassName(
                            it.packageName.asString(),
                            it.simpleName
                                .asString()
                                .builderClassName(),
                        ),
                    )
                    .build()

                addParameter(constructorParameter)
            }
        }
        .build()

    primaryConstructor(constructor)

    return this
}

private fun TypeSpec.Builder.addModuleBuildFunction(
    module: KSClassDeclaration,
    properties: List<KSPropertyDeclaration>,
): TypeSpec.Builder {
    val packageName = module.packageName.asString()
    val className = module.simpleName.asString()

    val primaryConstructor = module.primaryConstructor
        ?: error("Primary constructor not found for $className")

    val moduleBuilderFunction = FunSpec
        .builder("build")
        .returns(
            ClassName(
                packageName,
                className,
            ),
        )
        .addCode(
            buildCodeBlock {
                add(
                    "return %T(\n",
                    ClassName(
                        packageName,
                        className.overrideClassName(),
                    ),
                )
                indent()
                primaryConstructor.parameters.forEach { dependency ->
                    val modulePropertyName = dependency.name?.asString()
                    val moduleTypeName = dependency.type
                        .resolve()
                        .toClassName()
                        .simpleName
                        .replaceFirstChar(Char::lowercase)

                    add(
                        "%N = %N.build(),\n",
                        modulePropertyName,
                        moduleTypeName,
                    )
                }
                properties
                    .forEach { moduleProperty ->
                        add(
                            "%N = %N,\n",
                            moduleProperty.simpleName.asString() + "Lazy",
                            moduleProperty.simpleName.asString(),
                        )
                    }
                unindent()
                add(")\n")
            },
        )
        .build()

    addFunction(moduleBuilderFunction)

    return this
}

private fun TypeSpec.Builder.addDependentModules(
    moduleDependencies: List<KSClassDeclaration>,
): TypeSpec.Builder {
    moduleDependencies.forEach { dependency ->
        val dependencyClassName = dependency.toClassName()
        val dependencyPropertyName = dependency.toPropertyName()

        val dependencyBuilderClassName = dependencyClassName
            .simpleName
            .builderClassName()

        val dependencyBuilderProperty = PropertySpec
            .builder(
                dependencyPropertyName,
                ClassName(
                    dependencyClassName.packageName,
                    dependencyBuilderClassName,
                ),
                PUBLIC,
            )
            .mutable(false)
            .initializer(
                codeBlock = buildCodeBlock {
                    add(
                        "%N",
                        dependencyPropertyName,
                    )
                },
            )
            .build()

        addProperty(dependencyBuilderProperty)

        val moduleCustomizerFunction = FunSpec
            .builder(
                dependencyPropertyName,
            )
            .addParameter(
                "initializer",
                LambdaTypeName.get(
                    receiver = ClassName(
                        dependencyClassName.packageName,
                        dependencyBuilderClassName,
                    ),
                    returnType = Unit::class.asTypeName(),
                ),
            )
            .addCode(
                buildCodeBlock {
                    add(
                        "%N.apply(initializer)",
                        dependencyPropertyName,
                    )
                },
            )
            .build()

        addFunction(moduleCustomizerFunction)
    }

    return this
}

private fun TypeSpec.Builder.addModuleProperties(
    moduleProperties: List<KSPropertyDeclaration>,
): TypeSpec.Builder {
    moduleProperties
        .forEach { moduleProperty ->
            val backingLazyProperty = PropertySpec
                .builder(
                    moduleProperty.simpleName.asString(),
                    kotlinLazy
                        .parameterizedBy(
                            moduleProperty.type
                                .resolve()
                                .toClassName(),
                        )
                        .copy(nullable = true),
                    PRIVATE,
                )
                .mutable(true)
                .initializer("null")
                .build()

            addProperty(backingLazyProperty)

            val backingLazyPropertySetter = FunSpec
                .builder(
                    moduleProperty.simpleName.asString(),
                )
                .addParameter(
                    "initializer",
                    LambdaTypeName.get(
                        receiver = null,
                        returnType = moduleProperty.type
                            .resolve()
                            .toClassName(),
                    ),
                )
                .addCode(
                    buildCodeBlock {
                        add(
                            "%N = lazy(initializer)",
                            moduleProperty.simpleName.asString(),
                        )
                    },
                )
                .build()

            addFunction(backingLazyPropertySetter)
        }

    return this
}
