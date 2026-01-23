package io.heapy.komok.tech.di.delegate

import io.heapy.komok.tech.logging.logger
import kotlin.time.measureTimedValue

private fun traverseModuleTree(
    beans: MutableMap<Class<*>, Lazy<Any>>,
    type: Class<*>,
) {
    if (beans[type] == null) {
        val constructor = type.constructors.singleOrNull()
            ?: error("Primary constructor not found for $type")
        val parameters = constructor.parameters

        val createFunction = lazy(
            mode = LazyThreadSafetyMode.NONE,
        ) {
            val args = parameters
                .map { param ->
                    val paramType = param.type

                    beans[paramType]?.value
                }
                .toTypedArray()

            constructor.newInstance(*args)
        }

        beans[type] = createFunction

        parameters.forEach { param ->
            traverseModuleTree(beans, param.type)
        }
    }
}

inline fun <reified T : Any> buildModule(): T {
    return buildModule(T::class.java) as T
}

class ModuleRegistry
@PublishedApi
internal constructor(
    private val modules: Map<Class<*>, Lazy<Any>>,
) {
    inline operator fun <reified T : Any> invoke(): T {
        return get(T::class.java)
    }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> get(type: Class<T>): T {
        return modules[type]?.value as? T
            ?: error("Module of type $type not found in registry")
    }
}

inline fun <reified T : Any> buildModules(): ModuleRegistry {
    return buildModules(T::class.java)
}

@PublishedApi
internal fun buildModules(
    moduleType: Class<*>,
): ModuleRegistry {
    val log = logger {}

    val beanTreeResult = measureTimedValue {
        val beans = mutableMapOf<Class<*>, Lazy<Any>>()
        traverseModuleTree(beans, moduleType)
        beans
    }

    log.info("Constructing module tree for module {} taken {}", moduleType, beanTreeResult.duration)

    val result = measureTimedValue {
        // Force initialization of root module
        beanTreeResult.value[moduleType]?.value
            ?: error("Type $moduleType not found")
        ModuleRegistry(beanTreeResult.value)
    }

    log.info("Constructing module {} taken {}", moduleType, result.duration)

    return result.value
}

@PublishedApi
internal fun buildModule(
    moduleType: Class<*>,
): Any {
    val log = logger {}

    val beanTreeResult = measureTimedValue {
        val beans = mutableMapOf<Class<*>, Lazy<Any>>()
        traverseModuleTree(beans, moduleType)
        beans
    }

    log.info("Constructing module tree for module {} taken {}", moduleType, beanTreeResult.duration)

    val result = measureTimedValue {
        beanTreeResult.value[moduleType]?.value
            ?: error("Type $moduleType not found")
    }

    log.info("Constructing module {} taken {}", moduleType, result.duration)

    return result.value
}
