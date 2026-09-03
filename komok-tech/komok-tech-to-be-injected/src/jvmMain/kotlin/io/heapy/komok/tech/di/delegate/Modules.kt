package io.heapy.komok.tech.di.delegate

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.time.measureTimedValue

private val log = LoggerFactory.getLogger("io.heapy.komok.tech.di.delegate.Modules")

private fun traverseModuleTree(
    beans: MutableMap<KClass<*>, ModuleRegistry.() -> Any>,
    type: Class<*>,
) {
    val key = type.kotlin
    if (beans[key] == null) {
        val constructor = type.constructors.singleOrNull()
            ?: error("Primary constructor not found for $type")
        val parameters = constructor.parameters

        beans[key] = {
            val args = parameters
                .map { parameter ->
                    this[parameter.type.kotlin]
                }
                .toTypedArray()

            constructor.newInstance(*args)
        }

        parameters.forEach { parameter ->
            traverseModuleTree(beans, parameter.type)
        }
    }
}

/**
 * Reflectively builds [T] on the JVM.
 *
 * Other targets use the explicit-factory overload from common code.
 */
inline fun <reified T : Any> buildModule(): T =
    buildModule(T::class.java) as T

/**
 * Reflectively builds a registry rooted at [T] on the JVM.
 *
 * Other targets use the explicit-factory overload from common code.
 */
inline fun <reified T : Any> buildModules(): ModuleRegistry =
    buildModules(T::class.java)

@PublishedApi
internal fun buildModules(
    moduleType: Class<*>,
): ModuleRegistry {
    val beanTreeResult = measureTimedValue {
        val beans = mutableMapOf<KClass<*>, ModuleRegistry.() -> Any>()
        traverseModuleTree(beans, moduleType)
        beans
    }

    log.info(
        "Constructing module tree for module {} took {}",
        moduleType,
        beanTreeResult.duration,
    )

    val result = measureTimedValue {
        val registry = ModuleRegistry(beanTreeResult.value)
        val _ = registry[moduleType.kotlin]
        registry
    }

    log.info("Constructing module {} took {}", moduleType, result.duration)
    return result.value
}

@PublishedApi
internal fun buildModule(
    moduleType: Class<*>,
): Any {
    val modules = buildModules(moduleType)
    return modules[moduleType.kotlin]
}
