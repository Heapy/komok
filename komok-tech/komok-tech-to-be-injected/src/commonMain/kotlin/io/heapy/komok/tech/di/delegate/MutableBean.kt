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
            else -> {
                gate
                readInitialized()
            }
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

    private fun runInitializer() {
        val witness = state.compareAndExchange(
            expectedValue = BeanState.Uninitialized,
            newValue = BeanState.Initializing,
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

private sealed interface BeanState<out V> {
    object Uninitialized : BeanState<Nothing>

    object Initializing : BeanState<Nothing>

    class Initialized<V>(
        val value: V,
    ) : BeanState<V>
}
