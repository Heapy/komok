package io.heapy.komok.tech.runtime.compiler.sample

import io.heapy.komok.tech.runtime.mock
import io.heapy.komok.tech.runtime.spy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

interface Formatter {
    val prefix: String
    suspend fun format(name: String): String
    fun count(): Int
    fun fallback(): String = "default"
}

class RealFormatter : Formatter {
    override val prefix: String = "real"
    override suspend fun format(name: String): String = "$prefix:$name"
    override fun count(): Int = 7
    override fun fallback(): String = "override"
}

class MockTest {
    @Test
    fun `requested mock implements typed stubs and records arguments`() = runTest {
        val mock = mock<Formatter>()
        mock.every { format("") } answers { "mock:${it.arguments[0]}" }
        mock.every { prefix } returns "prefix"
        mock.every { count() } returns 42
        assertEquals("mock:Alice", mock.value.format("Alice"))
        assertEquals("prefix", mock.value.prefix)
        assertEquals(42, mock.value.count())
        assertEquals(listOf(listOf("Alice")), mock.calls { format("") }.map { it.arguments })
    }

    @Test
    fun `spy delegates default methods and unstubbed members`() = runTest {
        val spy = spy<Formatter>(RealFormatter())
        spy.every { format("") } returns "stub"
        assertEquals("stub", spy.value.format("Alice"))
        assertEquals("real", spy.value.prefix)
        assertEquals(7, spy.value.count())
        assertEquals("override", spy.value.fallback())
    }
}

private interface GenericBox<T> {
    suspend fun get(value: T): T
    fun record(value: T)
}

class GenericMockTest {
    @org.junit.jupiter.api.Test
    fun `generic interfaces support nullable suspend answers and Unit methods`() = kotlinx.coroutines.test.runTest {
        val mock = io.heapy.komok.tech.runtime.mock<GenericBox<String?>>()
        mock.every { get(null) }.coAnswers { null }
        mock.every { record(null) }.returns(Unit)
        org.junit.jupiter.api.Assertions.assertNull(mock.value.get("value"))
        mock.value.record("recorded")
        org.junit.jupiter.api.Assertions.assertEquals(listOf(listOf("recorded")), mock.calls { record(null) }.map { it.arguments })
    }
}
