package io.heapy.komok.tech.lazy.const

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class LazyConstExtensionsTest {
    private class Holder(
        private val counter: Counter,
    ) {
        val value by lazyConst {
            counter.inc()
            Any()
        }
    }

    private class Counter(var value: Int = 0) {
        fun inc() {
            value += 1
        }
    }

    @Test
    fun `initializer is not called before first access and called once on first access`() {
        val counter = Counter()
        val holder = Holder(counter)

        // Before first access: should not initialize
        assertEquals(0, counter.value)

        // First access triggers initialization
        val v1 = holder.value
        assertEquals(1, counter.value)

        // Subsequent access should not reinitialize
        val v2 = holder.value
        assertEquals(1, counter.value)

        // Value should be stable (same instance)
        assertSame(v1, v2)
    }

    @Test
    fun `multiple holders have independent lazy const`() {
        val counter1 = Counter()
        val counter2 = Counter()
        val h1 = Holder(counter1)
        val h2 = Holder(counter2)

        val v1 = h1.value
        val v2 = h2.value

        assertEquals(1, counter1.value)
        assertEquals(1, counter2.value)
        // Different holders should hold different instances
        assertFalse(v1 === v2)
    }
}
