package io.heapy.komok.tech.di.ez.dsl

import io.heapy.komok.tech.di.ez.impl.ModuleBuilderPropertyDelegate
import io.heapy.komok.tech.di.ez.api.ModuleBuilder
import io.heapy.komok.tech.di.ez.api.ModuleDSL
import io.heapy.komok.tech.di.ez.api.ModuleProvider
import kotlin.properties.ReadOnlyProperty

@ModuleDSL
fun module(
    builder: ModuleBuilder,
): ReadOnlyProperty<Any?, ModuleProvider> {
    return ModuleBuilderPropertyDelegate(builder)
}
