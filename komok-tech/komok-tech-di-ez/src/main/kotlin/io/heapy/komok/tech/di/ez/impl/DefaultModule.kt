package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Binding
import io.heapy.komok.tech.di.ez.api.Module
import io.heapy.komok.tech.di.ez.api.ModuleProvider

internal class DefaultModule(
    override val source: String,
    override val dependencies: List<ModuleProvider>,
    override val bindings: List<Binding<*>>,
) : Module
