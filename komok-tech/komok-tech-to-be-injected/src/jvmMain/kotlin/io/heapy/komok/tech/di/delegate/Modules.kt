package io.heapy.komok.tech.di.delegate

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTimedValue

private val log = LoggerFactory.getLogger("io.heapy.komok.tech.di.delegate.Modules")

private fun traverseModuleTree(
    factories: MutableMap<Class<*>, ModuleRegistry.() -> Any>,
    type: Class<*>,
) {
    if (factories[type] == null) {
        val constructor = type.constructors.singleOrNull()
            ?: error("Primary constructor not found for $type")
        val parameters = constructor.parameters

        factories[type] = {
            val args = parameters
                .map { parameter -> get(parameter.type) }
                .toTypedArray()

            constructor.newInstance(*args)
        }

        parameters.forEach { parameter ->
            traverseModuleTree(factories, parameter.type)
        }
    }
}

/**
 * Registry of lazily-created modules, keyed by their class.
 */
class ModuleRegistry
private constructor(
    private val factories: Map<Class<*>, ModuleRegistry.() -> Any>,
    private val modules: AtomicReference<Map<Class<*>, Any>>,
    private val resolving: List<Class<*>>,
) {
    @PublishedApi
    internal constructor(
        factories: Map<Class<*>, ModuleRegistry.() -> Any>,
    ) : this(
        factories = factories,
        modules = AtomicReference(emptyMap()),
        resolving = emptyList(),
    )

    /**
     * Returns the registered module of type [T], creating it on first access.
     */
    inline operator fun <reified T : Any> invoke(): T = get(T::class.java)

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> get(type: Class<T>): T {
        check(type !in resolving) {
            "Module dependency cycle: ${(resolving + type).joinToString(" -> ")}"
        }

        val cached = modules.get()[type]
        if (cached != null) {
            return cached as T
        }

        val factory = factories[type]
            ?: error("Module of type $type not found in registry")
        val nested = ModuleRegistry(
            factories = factories,
            modules = modules,
            resolving = resolving + type,
        )

        return share(type, factory(nested)) as T
    }

    private fun share(
        type: Class<*>,
        candidate: Any,
    ): Any {
        while (true) {
            val current = modules.get()
            val winner = current[type]
            if (winner != null) {
                return winner
            }
            if (modules.compareAndSet(current, current + (type to candidate))) {
                return candidate
            }
        }
    }
}

/**
 * Reflectively builds [T] and every module its constructor needs.
 */
inline fun <reified T : Any> buildModule(): T = buildModule(T::class.java) as T

/**
 * Reflectively builds a registry rooted at [T].
 */
inline fun <reified T : Any> buildModules(): ModuleRegistry = buildModules(T::class.java)

@PublishedApi
internal fun buildModules(
    moduleType: Class<*>,
): ModuleRegistry {
    val factoryResult = measureTimedValue {
        val factories = mutableMapOf<Class<*>, ModuleRegistry.() -> Any>()
        traverseModuleTree(factories, moduleType)
        factories
    }

    log.info(
        "Constructing module tree for module {} took {}",
        moduleType,
        factoryResult.duration,
    )

    val result = measureTimedValue {
        val registry = ModuleRegistry(factoryResult.value)
        val _ = registry.get(moduleType)
        registry
    }

    log.info("Constructing module {} took {}", moduleType, result.duration)
    return result.value
}

@PublishedApi
internal fun buildModule(
    moduleType: Class<*>,
): Any = buildModules(moduleType).get(moduleType)
