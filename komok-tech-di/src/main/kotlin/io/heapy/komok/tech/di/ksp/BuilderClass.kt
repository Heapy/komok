package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.processing.KSPLogger
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
import com.squareup.kotlinpoet.ksp.toTypeName

fun String.builderClassName(): String =
    "${this}Builder"

fun String.toPropertyName(): String =
    replaceFirstChar(Char::lowercase)

fun KSClassDeclaration.toPropertyName(): String =
    simpleName
        .asString()
        .toPropertyName()

fun generateBuilderClass(
    logger: KSPLogger,
    module: KSClassDeclaration,
    moduleDependencies: List<KSClassDeclaration>,
    properties: List<KSPropertyDeclaration>,
): TypeSpec {
    val className = module.simpleName.asString()

    return TypeSpec
        .classBuilder(className.builderClassName())
        .addBuilderClassConstructor(
            logger,
            moduleDependencies,
        )
        .addDependentModules(
            logger,
            moduleDependencies,
        )
        .addModuleProperties(
            module,
            logger,
            properties,
        )
        .addModuleBuildFunction(
            logger,
            module,
            properties,
        )
        .build()
}

private fun TypeSpec.Builder.addBuilderClassConstructor(
    logger: KSPLogger,
    moduleDependencies: List<KSClassDeclaration>,
): TypeSpec.Builder {
    val constructor = try {
        FunSpec
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
    } catch (e: Exception) {
        logger.error("Failed to generate builder class constructor")
        throw e
    }

    primaryConstructor(constructor)

    return this
}

private fun TypeSpec.Builder.addModuleBuildFunction(
    logger: KSPLogger,
    module: KSClassDeclaration,
    properties: List<KSPropertyDeclaration>,
): TypeSpec.Builder {
    val packageName = module.packageName.asString()
    val className = module.simpleName.asString()

    val primaryConstructor = module.primaryConstructor
        ?: error("MBF: Primary constructor not found for $className")

    val moduleBuilderFunction = try {
        FunSpec
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
    } catch (e: Exception) {
        logger.error("Failed to generate module build function")
        throw e
    }

    addFunction(moduleBuilderFunction)

    return this
}

private fun TypeSpec.Builder.addDependentModules(
    logger: KSPLogger,
    moduleDependencies: List<KSClassDeclaration>,
): TypeSpec.Builder {
    moduleDependencies.forEach { dependency ->
        val dependencyClassName = dependency.toClassName()
        val dependencyPropertyName = dependency.toPropertyName()

        val dependencyBuilderClassName = dependencyClassName
            .simpleName
            .builderClassName()

        val dependencyBuilderProperty = try {
            PropertySpec
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
        } catch (e: Exception) {
            logger.error("Failed to generate dependency builder property")
            throw e
        }

        addProperty(dependencyBuilderProperty)

        val moduleCustomizerFunction = try {
            FunSpec
                .builder(
                    dependencyPropertyName,
                )
                .addAnnotation(moduleDslMarker)
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
        } catch (e: Exception) {
            logger.error("Failed to generate module customizer function")
            throw e
        }

        addFunction(moduleCustomizerFunction)
    }

    return this
}

private fun TypeSpec.Builder.addModuleProperties(
    module: KSClassDeclaration,
    logger: KSPLogger,
    moduleProperties: List<KSPropertyDeclaration>,
): TypeSpec.Builder {
    moduleProperties
        .forEach { moduleProperty ->
            val backingLazyProperty = try {
                PropertySpec
                    .builder(
                        moduleProperty.simpleName.asString(),
                        kotlinLazy
                            .parameterizedBy(
                                try {
                                    moduleProperty.type
                                        .resolve()
                                        .toTypeName()
                                } catch (e: Exception) {
                                    logger.error("""
                                        Failed to resolve module property type: ${module.packageName.asString()}.${module.simpleName.asString()}.${moduleProperty.simpleName.asString()}
                                        Defined in: ${module.location()}
                                        Please specify the type explicitly
                                        """.trimIndent())
                                    throw e
                                },
                            )
                            .copy(nullable = true),
                        PRIVATE,
                    )
                    .mutable(true)
                    .initializer("null")
                    .build()
            } catch (e: Exception) {
                logger.error("Failed to generate backing lazy property")
                throw e
            }

            addProperty(backingLazyProperty)

            val backingLazyPropertySetter = try {
                FunSpec
                    .builder(
                        moduleProperty.simpleName.asString(),
                    )
                    .addAnnotation(moduleDslMarker)
                    .addParameter(
                        "initializer",
                        LambdaTypeName.get(
                            receiver = null,
                            returnType = moduleProperty.type
                                .resolve()
                                .toTypeName(),
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
            } catch (e: Exception) {
                logger.error("Failed to generate backing lazy property setter")
                throw e
            }

            addFunction(backingLazyPropertySetter)
        }

    return this
}
