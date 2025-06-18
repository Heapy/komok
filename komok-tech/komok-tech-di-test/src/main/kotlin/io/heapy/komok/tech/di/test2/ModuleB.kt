package io.heapy.komok.tech.di.test2

import io.heapy.komok.tech.di.lib.Module

class B(
    private val a: A,
) {
    fun getA() = a
}

@Module
open class ModuleB(
    private val moduleA: ModuleA,
) {
    open val b by lazy {
        B(moduleA.a)
    }
}
