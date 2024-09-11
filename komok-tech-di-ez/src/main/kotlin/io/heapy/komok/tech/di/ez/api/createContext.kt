package io.heapy.komok.tech.di.ez.api

import io.heapy.komok.tech.di.ez.impl.KomokContext
import io.heapy.komok.tech.di.ez.impl.processBindings
import io.heapy.komok.tech.di.ez.impl.processModuleTree

fun ModuleProvider.createContext(): Context {
    val moduleProvider = this
    val bindings = mutableMapOf<Key, Binding<*>>()
    val processedModules = mutableMapOf<ModuleProvider, Module>()

    val rootModule = moduleProvider.module()
    processedModules[moduleProvider] = rootModule

    processModuleTree(
        parent = rootModule,
        modules = processedModules,
    )

    processedModules.forEach { (_, module) ->
        processBindings(
            bindings = bindings,
            module = module,
        )
    }

    return KomokContext(bindings.toMap())
}
