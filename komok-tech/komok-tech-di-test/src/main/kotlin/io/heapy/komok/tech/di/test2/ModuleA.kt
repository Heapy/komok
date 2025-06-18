package io.heapy.komok.tech.di.test2

import io.heapy.komok.tech.di.lib.Module

class A

@Module
open class ModuleA {
    open val a by lazy {
        A()
    }
}
