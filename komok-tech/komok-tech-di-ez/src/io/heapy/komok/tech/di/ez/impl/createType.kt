package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.GenericKey
import io.heapy.komok.tech.di.ez.api.Key
import io.heapy.komok.tech.di.ez.api.ProviderBinding
import kotlin.reflect.KClass

internal fun ExecutionContext.createType(
    key: Key,
    saveInstance: Boolean = true,
): Any? {
    if (stack.contains(key)) {
        val graph = printCircularDependencyGraph(
            key,
            stack,
            definitions,
        )
        throw ContextException(
            "A circular dependency found:$graph",
        )
    }

    val type = key.type
    val classifier = type.classifier
    val isOptional = type.isMarkedNullable

    if (classifier is KClass<*>) {
        if (classifier.objectInstance != null) {
            throw ContextException("Objects not allowed to be bound.")
        }
    }

    val binding = definitions[key]

    return if (binding == null) {
        if (isOptional) {
            null
        } else {
            throw ContextException("Required $key not found in context.")
        }
    } else {
        val alreadyCreated = key in instances

        if (alreadyCreated) {
            instances[key]
        } else {
            stack.add(key)
            // TODO: Don't create anything, but just save all actions and metadata,
            //   it will allow to implement dry-run and
            //   and create source code - equivalent of DI
            val instance = when (binding) {
                is ProviderBinding -> {
                    val params = binding.provider.parameters.associateWith { param ->
                        createType(
                            key = GenericKey<Any>(type = param.type),
                        )
                    }

                    binding.provider.callBy(params)
                }
            }
            stack.remove(key)

            if (saveInstance) {
                instances[key] = instance
            }

            instance
        }
    }
}
