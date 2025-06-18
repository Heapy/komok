package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Binding
import io.heapy.komok.tech.di.ez.api.Binder
import io.heapy.komok.tech.di.ez.api.ModuleProvider

internal class ContainerBinder(
    override val source: String,
) : Binder {
    val modules = mutableListOf<ModuleProvider>()
    val bindings = mutableListOf<Binding<*>>()

    override fun dependency(module: ModuleProvider) {
        modules.add(module)
    }

    override fun contribute(binding: Binding<*>) {
        bindings.add(binding)
    }
}
