package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Module
import io.heapy.komok.tech.di.ez.api.ModuleProvider

internal fun processModuleTree(
    parent: Module,
    modules: MutableMap<ModuleProvider, Module>,
) {
    parent.dependencies.forEach { moduleProvider ->
        val processedModule = modules[moduleProvider]
        if (processedModule == null) {
            val module = moduleProvider.module()
            modules[moduleProvider] = module

            if (module.dependencies.isNotEmpty()) {
                processModuleTree(
                    parent = module,
                    modules = modules,
                )
            }
        }
    }
}
