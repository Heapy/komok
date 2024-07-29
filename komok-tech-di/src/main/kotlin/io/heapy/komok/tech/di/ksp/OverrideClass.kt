package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

fun String.overrideClassName(): String =
    "${this}Override"

fun generateOverrideClass(
    module: KSClassDeclaration,
    properties: List<KSPropertyDeclaration>,
): TypeSpec {
    val className = module.simpleName.asString()
    val packageName = module.packageName.asString()

    val primaryConstructor = module.primaryConstructor
        ?: error("Primary constructor not found for $className")

    val constructor = FunSpec
        .constructorBuilder()
        .apply {
            primaryConstructor.parameters.forEach {
                addParameter(
                    it.name?.asString()
                        ?: error("Parameter name not found"),
                    it.type
                        .resolve()
                        .toClassName(),
                )
            }

            properties
                .forEach {
                    addParameter(
                        "${it.simpleName.asString()}Lazy",
                        kotlinLazy
                            .parameterizedBy(
                                it.type
                                    .resolve()
                                    .toTypeName(),
                            )
                            .copy(nullable = true),
                    )
                }

        }
        .build()

    val companion = TypeSpec
        .companionObjectBuilder()
        .addProperty(
            PropertySpec
                .builder(
                    "log",
                    slf4jLogger,
                    PUBLIC,
                )
                .initializer(
                    "%T.getLogger(%T::class.java)",
                    slf4jLoggerFactory,
                    ClassName(
                        packageName,
                        className,
                    ),
                )
                .build(),
        )
        .build()

    val overrideClassName = "${className}Override"

    return TypeSpec
        .classBuilder(overrideClassName)
        .addModifiers(PRIVATE)
        .primaryConstructor(constructor)
        .superclass(
            ClassName(
                packageName,
                className,
            ),
        )
        .addType(companion)
        .addSuperclassConstructorParameter(
            buildCodeBlock {
                add("\n")
                indent()
                primaryConstructor.parameters.forEach {
                    add(
                        "%N = %N,\n",
                        it.name?.asString(),
                        it.name?.asString(),
                    )
                }
                unindent()
            },
        )
        .apply {
            properties
                .forEach { moduleProperty ->
                    val spec = PropertySpec
                        .builder(
                            moduleProperty.simpleName.asString(),
                            moduleProperty.type
                                .resolve()
                                .toTypeName(),
                            OVERRIDE,
                        )
                        .delegate(
                            buildCodeBlock {
                                add("lazy {\n")
                                indent()
                                add("val result = kotlin.time.measureTimedValue {\n")
                                indent()
                                add(
                                    "%N?.value ?: super.%N\n",
                                    moduleProperty.simpleName.asString() + "Lazy",
                                    moduleProperty.simpleName.asString(),
                                )
                                unindent()
                                add("}\n")
                                add(
                                    "log.debug(\"init %N: {}\", result.duration)\n",
                                    moduleProperty.simpleName.asString(),
                                )
                                add("result.value\n")
                                unindent()
                                add("}\n")
                            },
                        )
                        .build()

                    addProperty(spec)
                }
        }
        .build()
}
