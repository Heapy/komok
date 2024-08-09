package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration

fun dependencies(
    mainClass: KSClassDeclaration,
    dependencies: List<KSClassDeclaration>,
): Dependencies {
    val sources = (dependencies + mainClass)
        .mapNotNull { declaration -> declaration.containingFile }
        .toSet()
        .toTypedArray()

    return Dependencies(
        aggregating = true,
        sources = sources,
    )
}
