package io.heapy.komok.tech.di.delegate

import io.heapy.komok.tech.logging.Logger
import kotlin.time.measureTimedValue

/**
 * A mutable bean that initializes lazily.
 * The value of the bean can be mocked for testing purposes, before initialization.
 * The value of the bean can be set directly, before initialization.
 *
 * Uses classic DCL, implementation similar to [SynchronizedLazyImpl].
 */
class MutableBean<V> internal constructor(
    initializer: () -> V,
    private var name: String,
) {
    private var initializer: (() -> V)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    // final field is required to enable safe publication of constructed instance
    private val lock = this

    val value: V
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as V
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST") (_v2 as V)
                } else {
                    val typedValue = try {
                        measureTimedValue(initializer!!).let { timedValue ->
                            log.info("Initializing bean {} took {}", name, timedValue.duration)
                            timedValue.value
                        }
                    } catch (e: Exception) {
                        log.error("Error initializing bean $name", e)
                        throw e
                    }
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    val isInitialized: Boolean
        get() = _value !== UNINITIALIZED_VALUE

    /**
     * Mock the bean with a custom function.
     * This is useful for testing purposes.
     */
    fun mock(
        mockFn: () -> V,
    ): V {
        if (isInitialized) {
            error("Bean $name is already initialized with value $_value")
        } else {
            val result = measureTimedValue(mockFn)
            log.info("Mocking bean {} took {}", name, result.duration)
            _value = result.value
            return result.value
        }
    }

    /**
     * Set the value of the bean directly.
     * This is useful for testing purposes.
     */
    fun setValue(value: V) {
        if (isInitialized) {
            error("Bean $name is already initialized with value $_value")
        } else {
            log.info("Setting new value to bean $name")
            _value = value
        }
    }

    override fun toString(): String {
        return if (isInitialized) {
            value.toString()
        } else {
            "MutableBean $name value not initialized yet."
        }
    }

    private companion object : Logger()
}
