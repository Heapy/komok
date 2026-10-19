package io.heapy.komok.tech.runtime.externalmock

import io.heapy.komok.tech.runtime.*
import io.heapy.komok.tech.runtime.test.wrapper.ExternalFormatter
import io.heapy.komok.tech.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExternalGenerateMockTest {
    /** #13 — @GenerateMock emits a usable mock factory for a type not discovered via providers. */
    @Test
    fun `explicit generated mock stubs and verifies an otherwise unwrapped dependency type`() {
        val mock = mock<ExternalFormatter>()
        mock.every { format("") }.answers { "mock:${it.arguments[0]}" }

        val formatted = mock.value.format("invoice")

        assertAll(
            { assertEquals("mock:invoice", formatted) },
            { assertEquals(List(1) { listOf("invoice") }, mock.calls { format("") }.map { it.arguments }) },
        )
    }

    /** #13 — @GenerateMock emits a usable spy factory that delegates until a method is stubbed. */
    @Test
    fun `explicit generated spy delegates then allows targeted stubbing`() {
        val spy = spy<ExternalFormatter>(ExternalFormatter { value -> "real:$value" })

        val delegated = spy.value.format("receipt")
        spy.every { format("") }.answers { "stub:${it.arguments[0]}" }
        val stubbed = spy.value.format("receipt")

        assertAll(
            { assertEquals("real:receipt", delegated) },
            { assertEquals("stub:receipt", stubbed) },
            { assertEquals(List(2) { listOf("receipt") }, spy.calls { format("") }.map { it.arguments }) },
        )
    }

    /** #13 — @GenerateMock works for dependency interfaces with properties as well as functions. */
    @Test
    fun `explicit generated mock supports dependency interface properties`() {
        val clock = Clock.fixed(Instant.parse("2024-01-02T03:04:05Z"), ZoneOffset.UTC)
        val instant = Instant.parse("2025-02-03T04:05:06Z")
        val mock = mock<TimeSource>()
        mock.every { this.clock }.returns(clock)
        mock.every { instant() }.returns(instant)

        val actualClock = mock.value.clock
        val actualInstant = mock.value.instant()

        assertAll(
            { assertSame(clock, actualClock) },
            { assertEquals(instant, actualInstant) },
            { assertEquals(1, mock.calls { this.clock }.size) },
            { assertEquals(1, mock.calls { instant() }.size) },
        )
    }

    /** #13 — generated spies delegate dependency properties unless they are stubbed. */
    @Test
    fun `explicit generated spy delegates dependency interface properties and stubs functions`() {
        val clock = Clock.fixed(Instant.parse("2026-03-04T05:06:07Z"), ZoneOffset.UTC)
        val spy = spy<TimeSource>(TimeSource(clock))
        spy.every { localDate() }.returns(LocalDate.parse("2030-01-01"))

        val actualClock = spy.value.clock
        val actualInstant = spy.value.instant()
        val stubbedDate = spy.value.localDate()

        assertAll(
            { assertSame(clock, actualClock) },
            { assertEquals(clock.instant(), actualInstant) },
            { assertEquals(LocalDate.parse("2030-01-01"), stubbedDate) },
            { assertEquals(1, spy.calls { this.clock }.size) },
            { assertEquals(1, spy.calls { instant() }.size) },
            { assertEquals(1, spy.calls { localDate() }.size) },
        )
    }

    /** #13 — typed factory objects provide a reflection-free `mock(FooMockFactory)` DSL. */
    @Test
    fun `typed factory object creates explicit generated mocks`() {
        val formatter = mock<ExternalFormatter>()
        val timeSource = mock<TimeSource>()
        val clock = Clock.fixed(Instant.parse("2027-04-05T06:07:08Z"), ZoneOffset.UTC)
        formatter.every { format("") }.answers { "factory:${it.arguments[0]}" }
        timeSource.every { this.clock }.returns(clock)

        assertAll(
            { assertEquals("factory:note", formatter.value.format("note")) },
            { assertSame(clock, timeSource.value.clock) },
            { assertEquals(List(1) { listOf("note") }, formatter.calls { format("") }.map { it.arguments }) },
            { assertEquals(1, timeSource.calls { this.clock }.size) },
        )
    }

    /** #13 — typed factory objects also create spies without runtime type lookup. */
    @Test
    fun `typed factory object creates explicit generated spies`() {
        val formatter = spy<ExternalFormatter>(
            ExternalFormatter { value -> "real:$value" },
        )

        assertAll(
            { assertEquals("real:note", formatter.value.format("note")) },
            { assertEquals(List(1) { listOf("note") }, formatter.calls { format("") }.map { it.arguments }) },
        )
    }

    /** #13 — generated call logs are per-instance and safe under parallel test calls. */
    @Test
    fun `generated mock verification is parallel safe`() = runTest {
        val formatter = mock<ExternalFormatter>()
        formatter.every { format("") }.answers { it.arguments[0] as String }

        coroutineScope {
            repeat(200) {
                launch(Dispatchers.Default) {
                    check(formatter.value.format("parallel") == "parallel")
                }
            }
        }

        assertEquals(List(200) { listOf("parallel") }, formatter.calls { format("") }.map { it.arguments })
    }
}
