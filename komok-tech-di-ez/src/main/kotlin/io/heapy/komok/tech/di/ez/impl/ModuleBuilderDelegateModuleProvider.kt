package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Module
import io.heapy.komok.tech.di.ez.api.ModuleBuilder
import io.heapy.komok.tech.di.ez.api.ModuleProvider

internal data class ModuleBuilderDelegateModuleProvider(
    private val builder: ModuleBuilder,
    private val source: String,
) : ModuleProvider {
    override fun module(): Module {
        val binder = ContainerBinder(source)
        this.builder.invoke(binder)

        return DefaultModule(
            source = source,
            dependencies = binder.modules,
            bindings = binder.bindings,
        )
    }

    override fun toString(): String {
        return "ModuleBuilderDelegateModuleProvider(source='$source')"
    }
}
