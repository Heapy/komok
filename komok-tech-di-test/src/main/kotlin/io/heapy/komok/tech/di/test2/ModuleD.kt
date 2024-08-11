package io.heapy.komok.tech.di.test2

import io.heapy.komok.tech.di.lib.Module

class D(
    private val a: A,
    private val b: B,
    private val c: C,
) {
    fun getAA() = a
    fun getAB() = b.getA()
    fun getAC() = c.getA()
}

@Module
open class ModuleD(
    private val moduleA: ModuleA,
    private val moduleB: ModuleB,
    private val moduleC: ModuleC,
) {
    open val d by lazy {
        D(moduleA.a, moduleB.b, moduleC.c)
    }
}
