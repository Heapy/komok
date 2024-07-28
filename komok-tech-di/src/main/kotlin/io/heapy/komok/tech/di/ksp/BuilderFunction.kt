package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock

private fun CodeBlock.Builder.initializeModuleBuilder(
    graph: Map<KSClassDeclaration, List<KSClassDeclaration>>,
    module: KSClassDeclaration,
) {
    val packageName = module.packageName.asString()
    val className = module.simpleName.asString()
    val propertyName = module.toPropertyName()

    add(
        "val %N = %T(\n",
        propertyName,
        ClassName(
            packageName,
            "${className}Builder",
        ),
    )
    indent()
    val moduleDependencies = graph[module] ?: emptySet()
    moduleDependencies.forEach { moduleDependency ->
        add(
            "%N = %N,\n",
            moduleDependency.toPropertyName(),
            moduleDependency.toPropertyName(),
        )
    }
    unindent()
    add(")\n")
}

fun generateBuilderFunction(
    graph: Map<KSClassDeclaration, List<KSClassDeclaration>>,
    module: KSClassDeclaration,
    moduleDependencies: List<KSClassDeclaration>,
    sortedGraph: List<KSClassDeclaration>,
): FunSpec {
    val className = module.simpleName.asString()
    val packageName = module.packageName.asString()

    return FunSpec
        .builder("create$className")
        .addParameter(
            "builder",
            LambdaTypeName.get(
                receiver = ClassName(
                    packageName,
                    "${className}Builder",
                ),
                returnType = Unit::class.asTypeName(),
            ),
        )
        .returns(
            ClassName(
                packageName,
                className,
            ),
        )
        .addCode(
            buildCodeBlock {
                sortedGraph
                    .dropWhile { it != module }
                    .drop(1)
                    .reversed()
                    .filter { it in moduleDependencies }
                    .forEach { dependency ->
                        initializeModuleBuilder(
                            graph,
                            dependency,
                        )
                    }

                add(
                    "return %T(\n",
                    ClassName(
                        packageName,
                        "${className}Builder",
                    ),
                )
                indent()
                moduleDependencies.forEach { moduleDependency ->
                    add(
                        "%N = %N,\n",
                        moduleDependency.toPropertyName(),
                        moduleDependency.toPropertyName(),
                    )
                }
                unindent()

                add(").apply(builder).build()\n")
            },
        )
        .build()
}
