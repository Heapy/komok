package io.heapy.komok.tech.di.delegate

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

/**
 * Registry of lazily-created modules, keyed by their Kotlin class.
 */
@OptIn(ExperimentalAtomicApi::class)
class ModuleRegistry
private constructor(
    private val factories: Map<KClass<*>, ModuleRegistry.() -> Any>,
    private val modules: AtomicReference<Map<KClass<*>, Lazy<Any>>>,
    private val resolving: List<KClass<*>>,
) {
    internal constructor(
        factories: Map<KClass<*>, ModuleRegistry.() -> Any>,
    ) : this(
        factories = factories,
        modules = AtomicReference(emptyMap()),
        resolving = emptyList(),
    )

    /**
     * Returns the registered module of type [T].
     */
    inline operator fun <reified T : Any> invoke(): T = this[T::class]

    /**
     * Returns the registered module identified by [type].
     *
     * The module is created on first access.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(type: KClass<T>): T {
        check(type !in resolving) {
            "Module dependency cycle: ${(resolving + type).joinToString(" -> ")}"
        }

        val cached = modules.load()[type]
        if (cached != null) {
            return cached.value as T
        }

        val factory = factories[type]
            ?: error("Module of type $type not found in registry")
        val nested = ModuleRegistry(
            factories = factories,
            modules = modules,
            resolving = resolving + type,
        )

        return share(type, lazy { factory(nested) }).value as T
    }

    private fun share(
        type: KClass<*>,
        candidate: Lazy<Any>,
    ): Lazy<Any> {
        while (true) {
            val current = modules.load()
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
 * Builds a portable module graph from explicit module factories.
 *
 * Factories are lazy and may access other registered modules through the
 * [ModuleRegistry] receiver.
 */
class ModuleGraphBuilder {
    private val factories = mutableMapOf<KClass<*>, ModuleRegistry.() -> Any>()

    /**
     * Registers one factory for module type [T].
     */
    inline fun <reified T : Any> module(
        noinline factory: ModuleRegistry.() -> T,
    ) {
        module(T::class, factory)
    }

    /**
     * Registers one factory for [type].
     */
    fun <T : Any> module(
        type: KClass<T>,
        factory: ModuleRegistry.() -> T,
    ) {
        val previous = factories.put(type, factory)
        require(previous == null) {
            "Module factory for $type is already registered"
        }
    }

    internal fun buildFactories(): Map<KClass<*>, ModuleRegistry.() -> Any> =
        factories.toMap()
}

/**
 * Builds a module registry using factories that work on every Kotlin target.
 *
 * A dependency cycle fails on first access with the cycle path in the message.
 */
fun buildModules(
    graph: ModuleGraphBuilder.() -> Unit,
): ModuleRegistry =
    ModuleRegistry(
        ModuleGraphBuilder()
            .apply(graph)
            .buildFactories(),
    )

/**
 * Builds and returns root module [T] using portable explicit factories.
 */
inline fun <reified T : Any> buildModule(
    noinline graph: ModuleGraphBuilder.() -> Unit,
): T = buildModules(graph)()
