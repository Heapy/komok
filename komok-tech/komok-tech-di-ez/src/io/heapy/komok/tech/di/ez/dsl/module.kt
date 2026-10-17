package io.heapy.komok.tech.di.ez.dsl

import io.heapy.komok.tech.di.ez.api.ModuleBuilder
import io.heapy.komok.tech.di.ez.api.ModuleProvider
import io.heapy.komok.tech.di.ez.impl.ModuleBuilderPropertyDelegate
import kotlin.properties.ReadOnlyProperty

fun module(
    builder: ModuleBuilder,
): ReadOnlyProperty<Any?, ModuleProvider> {
    return ModuleBuilderPropertyDelegate(builder)
}
