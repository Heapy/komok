package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevelTest {
    @Test
    fun `levels are ordered by severity`() {
        val levels = Level.entries
        assertEquals(
            listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.OFF),
            levels,
        )
    }

    @Test
    fun `ordinal comparison works for filtering`() {
        assertTrue(Level.ERROR.ordinal >= Level.INFO.ordinal)
        assertTrue(Level.INFO.ordinal >= Level.TRACE.ordinal)
        assertTrue(Level.OFF.ordinal >= Level.ERROR.ordinal)
    }

    @Test
    fun `OFF has highest ordinal`() {
        for (level in Level.entries) {
            assertTrue(Level.OFF.ordinal >= level.ordinal)
        }
    }
}
