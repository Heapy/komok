package io.heapy.komok.tech.lazy.const

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class LazyCollectionsTest {
    @Test
    fun `lazyList maps indices once and exposes stable content`() {
        var calls = 0
        val list = lazyList(5) { i ->
            calls += 1
            "v$i"
        }
        // size and content
        assertEquals(5, list.size)
        assertEquals(listOf("v0", "v1", "v2", "v3", "v4"), list)
        // accessing again should not invoke mapper again (list is already built by StableValue.list)
        val snapshot = list.toList()
        assertEquals(listOf("v0", "v1", "v2", "v3", "v4"), snapshot)
        // Mapper should have been called exactly 5 times
        assertEquals(5, calls)
    }

    @Test
    fun `lazyMap computes once per key and is stable`() {
        var calls = 0
        val keys = linkedSetOf("a", "b", "c") // preserve order for predictable assertions
        val map = lazyMap(keys) { k ->
            calls += 1
            k.uppercase()
        }

        // Check values
        assertEquals("A", map["a"]) // accessing should not recompute on subsequent calls
        assertEquals("B", map["b"])
        assertEquals("C", map["c"])
        // Re-access
        assertEquals("A", map["a"])
        assertEquals("B", map["b"])
        assertEquals("C", map["c"])

        // Exactly 3 computations expected
        assertEquals(3, calls)

        // Map stability: same instance values for repeated access
        val va1 = map["a"]
        val va2 = map["a"]
        assertSame(va1, va2)
    }
}
