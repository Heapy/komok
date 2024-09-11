package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Binding
import io.heapy.komok.tech.di.ez.api.ProviderBinding
import io.heapy.komok.tech.di.ez.api.Key

internal fun printCircularDependencyGraph(
    key: Key,
    stack: MutableList<Key>,
    bindings: Map<Key, Binding<*>>,
): String {
    return buildString {
        appendLine()
        stack.forEachIndexed { idx, stackKey ->
            append(" ".repeat(idx * 2))
            append(stackKey.type.classifier)
            bindings[stackKey]?.let {
                append(" implemented by ")
                val desc = when (it) {
                    is ProviderBinding -> "provider [${it.provider::class}]"
                }
                append(desc)
            }
            if (stackKey == key) {
                appendLine(" <-- Circular dependency starts here")
            } else {
                appendLine()
            }
        }
        append(" ".repeat(stack.size * 2))
        append(key.type.classifier)
        bindings[key]?.let {
            append(" implemented by ")
            val desc = when (it) {
                is ProviderBinding -> "provider [${it.provider::class}]"
            }
            append(desc)
        }
    }
}
