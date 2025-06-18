package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.ModuleBuilder
import io.heapy.komok.tech.di.ez.api.ModuleProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class ModuleBuilderPropertyDelegate(
    private val builder: ModuleBuilder,
) : ReadOnlyProperty<Any?, ModuleProvider> {
    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): ModuleProvider {
        val fullName = builder::class.toString()
        val variableName = property.name
        val place = fullName.substringBefore("$\$Lambda")
        val source = "$place.$variableName"

        return ModuleBuilderDelegateModuleProvider(
            builder = builder,
            source = source,
        )
    }
}
