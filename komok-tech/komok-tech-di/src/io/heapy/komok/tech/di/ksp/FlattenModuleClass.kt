package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun String.flattenClassName(): String =
    "${this}Flatten"

fun generateFlattenModuleClass(
    module: KSClassDeclaration,
    moduleDependencies: List<KSClassDeclaration>,
): TypeSpec {
    val className = module.simpleName.asString()

    return TypeSpec
        .classBuilder(className.flattenClassName())
        .addFlattenModuleClassConstructor(moduleDependencies + module)
        .build()
}

private fun TypeSpec.Builder.addFlattenModuleClassConstructor(
    moduleDependencies: List<KSClassDeclaration>,
): TypeSpec.Builder {
    val constructor = FunSpec
        .constructorBuilder()
        .apply {
            moduleDependencies.forEach { dependency ->
                val constructorParameter = ParameterSpec
                    .builder(
                        dependency.toPropertyName(),
                        ClassName(
                            dependency.packageName.asString(),
                            dependency.simpleName
                                .asString()
                        ),
                    )
                    .build()

                addParameter(constructorParameter)

                val property = PropertySpec
                    .builder(
                        dependency.toPropertyName(),
                        ClassName(
                            dependency.packageName.asString(),
                            dependency.simpleName
                                .asString()
                        ),
                    )
                    .initializer(dependency.toPropertyName())
                    .build()

                addProperty(property)
            }
        }
        .build()

    primaryConstructor(constructor)

    return this
}
