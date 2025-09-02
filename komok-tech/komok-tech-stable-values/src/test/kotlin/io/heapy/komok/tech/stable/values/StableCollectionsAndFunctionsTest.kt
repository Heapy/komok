package io.heapy.komok.tech.stable.values

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StableCollectionsAndFunctionsTest {
    @Test
    fun `stableList maps indices once and exposes stable content`() {
        var calls = 0
        val list = stableList(5) { i ->
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
    fun `stableMap computes once per key and is stable`() {
        var calls = 0
        val keys = linkedSetOf("a", "b", "c") // preserve order for predictable assertions
        val map = stableMap(keys) { k ->
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

    @Test
    fun `stableFunction memoizes results for provided domain`() {
        var calls = 0
        val domain = setOf(1, 2, 3)
        val f = stableFunction(domain) { x ->
            calls += 1
            "x=$x"
        }

        // First round
        assertEquals("x=1", f.apply(1))
        assertEquals("x=2", f.apply(2))
        assertEquals("x=3", f.apply(3))
        // Second round (should be memoized)
        assertEquals("x=1", f.apply(1))
        assertEquals("x=2", f.apply(2))
        assertEquals("x=3", f.apply(3))

        assertEquals(3, calls)
    }

    @Test
    fun `stableIntFunction memoizes per index within size`() {
        var calls = 0
        val f = stableIntFunction(4) { i ->
            calls += 1
            i * 10
        }

        // First pass 0..3
        assertEquals(0, f.apply(0))
        assertEquals(10, f.apply(1))
        assertEquals(20, f.apply(2))
        assertEquals(30, f.apply(3))
        // Repeat
        assertEquals(0, f.apply(0))
        assertEquals(10, f.apply(1))
        assertEquals(20, f.apply(2))
        assertEquals(30, f.apply(3))

        assertEquals(4, calls)
    }
}
