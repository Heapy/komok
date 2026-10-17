package io.heapy.komok.tech.di.delegate

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.measureTimedValue

/**
 * A thread-safe mutable bean that initializes lazily.
 *
 * The value can be mocked or set directly, but only before the initializer
 * starts. Once initialization is in progress, both calls fail.
 */
@OptIn(ExperimentalAtomicApi::class)
class MutableBean<V> internal constructor(
    private val initializer: () -> V,
    private val name: String,
) {
    private val state = AtomicReference<BeanState<V>>(BeanState.Uninitialized)

    /**
     * Lets one caller run the initializer and makes the others wait for it.
     */
    private val gate by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runInitializer()
    }

    val value: V
        get() = when (val current = state.load()) {
            is BeanState.Initialized -> current.value
            else -> awaitInitialized()
        }

    val isInitialized: Boolean
        get() = state.load() is BeanState.Initialized

    /**
     * Mocks the bean before its initializer starts.
     */
    fun mock(
        mockFn: () -> V,
    ): V {
        rejectOverride(state.load())

        val result = measureTimedValue(mockFn)
        publish(result.value)

        logBeanInfo("Mocking bean $name took ${result.duration}")
        return result.value
    }

    /**
     * Sets the bean before its initializer starts.
     */
    fun setValue(value: V) {
        publish(value)

        logBeanInfo("Setting new value to bean $name")
    }

    override fun toString(): String = when (val current = state.load()) {
        is BeanState.Initialized -> current.value.toString()
        else -> "MutableBean $name value not initialized yet."
    }

    private fun awaitInitialized(): V {
        val token = currentThreadToken()
        val outer = BlockedBeans.swap(token, this)

        try {
            rejectCycleAcrossThreads(token)
            gate
            return readInitialized()
        } finally {
            val _ = BlockedBeans.swap(token, outer)
        }
    }

    /**
     * Walks the wait-for chain that starts at this bean. Blocking on a bean
     * whose owner waits, directly or not, for a bean owned by [token] would
     * deadlock, because the initializer runs under [gate].
     */
    private fun rejectCycleAcrossThreads(token: Any) {
        val visited = mutableSetOf<Any>()
        var current: MutableBean<*> = this

        while (true) {
            val owner = (current.state.load() as? BeanState.Initializing)?.owner
                ?: return
            if (owner == token) {
                check(current === this) {
                    "Bean $name and bean ${current.name} form a dependency cycle across threads"
                }
                return
            }
            if (!visited.add(owner)) {
                return
            }
            current = BlockedBeans.of(owner)
                ?: return
        }
    }

    private fun runInitializer() {
        val witness = state.compareAndExchange(
            expectedValue = BeanState.Uninitialized,
            newValue = BeanState.Initializing(currentThreadToken()),
        )
        if (witness !is BeanState.Uninitialized) {
            check(witness !is BeanState.Initializing) {
                "Bean $name reads itself during initialization"
            }
            return
        }

        val result = try {
            measureTimedValue(initializer)
        } catch (throwable: Throwable) {
            state.store(BeanState.Uninitialized)
            logBeanError("Error initializing bean $name", throwable)
            throw throwable
        }

        state.store(BeanState.Initialized(result.value))
        logBeanInfo("Initializing bean $name took ${result.duration}")
    }

    private fun publish(value: V) {
        rejectOverride(
            state.compareAndExchange(
                expectedValue = BeanState.Uninitialized,
                newValue = BeanState.Initialized(value),
            ),
        )
    }

    private fun rejectOverride(witness: BeanState<V>) {
        when (witness) {
            is BeanState.Uninitialized -> Unit
            is BeanState.Initializing -> error("Bean $name is already initializing")
            is BeanState.Initialized -> error("Bean $name is already initialized")
        }
    }

    private fun readInitialized(): V = when (val current = state.load()) {
        is BeanState.Initialized -> current.value
        else -> error("Bean $name remained uninitialized")
    }
}

/**
 * Beans that threads are blocked on, so that a waiter can see the chain it is
 * about to join.
 */
@OptIn(ExperimentalAtomicApi::class)
private object BlockedBeans {
    private val entries = AtomicReference<Map<Any, MutableBean<*>>>(emptyMap())

    fun of(token: Any): MutableBean<*>? = entries.load()[token]

    fun swap(
        token: Any,
        bean: MutableBean<*>?,
    ): MutableBean<*>? {
        while (true) {
            val current = entries.load()
            val previous = current[token]
            val updated = if (bean == null) {
                current - token
            } else {
                current + (token to bean)
            }
            if (entries.compareAndSet(current, updated)) {
                return previous
            }
        }
    }
}

private sealed interface BeanState<out V> {
    object Uninitialized : BeanState<Nothing>

    class Initializing(
        val owner: Any,
    ) : BeanState<Nothing>

    class Initialized<V>(
        val value: V,
    ) : BeanState<V>
}
