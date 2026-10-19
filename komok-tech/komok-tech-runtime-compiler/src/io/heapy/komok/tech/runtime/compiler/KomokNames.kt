package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal object KomokNames {
    val runtimePackage = FqName("io.heapy.komok.tech.runtime")
    val module = ClassId.topLevel(runtimePackage.child(org.jetbrains.kotlin.name.Name.identifier("Module")))
    val context = ClassId.topLevel(runtimePackage.child(org.jetbrains.kotlin.name.Name.identifier("KomokContext")))
    val startup = ClassId.topLevel(runtimePackage.child(org.jetbrains.kotlin.name.Name.identifier("Startup")))
    val shutdown = ClassId.topLevel(runtimePackage.child(org.jetbrains.kotlin.name.Name.identifier("Shutdown")))
}
