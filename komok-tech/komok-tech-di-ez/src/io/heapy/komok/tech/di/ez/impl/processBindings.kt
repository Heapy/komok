package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Binding
import io.heapy.komok.tech.di.ez.api.Key
import io.heapy.komok.tech.di.ez.api.Module

internal fun processBindings(
    bindings: MutableMap<Key, Binding<*>>,
    module: Module,
) {
    module.bindings.forEach { binding ->
        val processedBinding = bindings[binding.key]

        if (processedBinding == null) {
            bindings[binding.key] = binding
        } else {
            val processed = processedBinding.source
            val current = module.source

            if (current == processed) {
                throw ContextException("Binding [${processedBinding.key}] duplicated in module [$current].")
            } else {
                throw ContextException(
                    "Binding [${processedBinding.key}] already present in module " + "[$processed]. Current module: [$current]",
                )
            }
        }
    }
}
