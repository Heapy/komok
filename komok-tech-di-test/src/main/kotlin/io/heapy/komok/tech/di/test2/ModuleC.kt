package io.heapy.komok.tech.di.test2

import io.heapy.komok.tech.di.lib.Module

class C(
    private val a: A,
) {
    fun getA() =
        a
}

@Module
open class ModuleC(
    private val moduleA: ModuleA,
) {
    open val c by lazy {
        C(moduleA.a)
    }
}
