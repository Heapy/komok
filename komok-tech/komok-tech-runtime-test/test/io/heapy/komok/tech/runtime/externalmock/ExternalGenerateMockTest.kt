package io.heapy.komok.tech.runtime.externalmock

import io.heapy.komok.tech.runtime.test.wrapper.ExternalFormatter
import io.heapy.komok.tech.runtime.test.wrapper.ExternalFormatterMockFactory
import io.heapy.komok.tech.runtime.test.wrapper.externalFormatterMock
import io.heapy.komok.tech.runtime.test.wrapper.externalFormatterSpy
import io.heapy.komok.tech.runtime.test.wrapper.mock as typedExternalFormatterMock
import io.heapy.komok.tech.runtime.test.wrapper.spy as typedExternalFormatterSpy
import io.heapy.komok.tech.time.TimeSource
import io.heapy.komok.tech.time.TimeSourceMockFactory
import io.heapy.komok.tech.time.timeSourceMock
import io.heapy.komok.tech.time.timeSourceSpy
import io.heapy.komok.tech.time.mock as typedTimeSourceMock
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
        val mock = externalFormatterMock()
        mock.everyFormat { value -> "mock:$value" }

        val formatted = mock.format("invoice")

        assertAll(
            { assertEquals("mock:invoice", formatted) },
            { mock.verifyFormat("invoice") },
        )
    }

    /** #13 — @GenerateMock emits a usable spy factory that delegates until a method is stubbed. */
    @Test
    fun `explicit generated spy delegates then allows targeted stubbing`() {
        val spy = externalFormatterSpy(ExternalFormatter { value -> "real:$value" })

        val delegated = spy.format("receipt")
        spy.everyFormat { value -> "stub:$value" }
        val stubbed = spy.format("receipt")

        assertAll(
            { assertEquals("real:receipt", delegated) },
            { assertEquals("stub:receipt", stubbed) },
            { spy.verifyFormat("receipt", times = 2) },
        )
    }

    /** #13 — @GenerateMock works for dependency interfaces with properties as well as functions. */
    @Test
    fun `explicit generated mock supports dependency interface properties`() {
        val clock = Clock.fixed(Instant.parse("2024-01-02T03:04:05Z"), ZoneOffset.UTC)
        val instant = Instant.parse("2025-02-03T04:05:06Z")
        val mock = timeSourceMock()
        mock.everyClock { clock }
        mock.everyInstant { instant }

        val actualClock = mock.clock
        val actualInstant = mock.instant()

        assertAll(
            { assertSame(clock, actualClock) },
            { assertEquals(instant, actualInstant) },
            { mock.verifyClock() },
            { mock.verifyInstant() },
        )
    }

    /** #13 — generated spies delegate dependency properties unless they are stubbed. */
    @Test
    fun `explicit generated spy delegates dependency interface properties and stubs functions`() {
        val clock = Clock.fixed(Instant.parse("2026-03-04T05:06:07Z"), ZoneOffset.UTC)
        val spy = timeSourceSpy(TimeSource(clock))
        spy.everyLocalDate { LocalDate.parse("2030-01-01") }

        val actualClock = spy.clock
        val actualInstant = spy.instant()
        val stubbedDate = spy.localDate()

        assertAll(
            { assertSame(clock, actualClock) },
            { assertEquals(clock.instant(), actualInstant) },
            { assertEquals(LocalDate.parse("2030-01-01"), stubbedDate) },
            { spy.verifyClock() },
            { spy.verifyInstant() },
            { spy.verifyLocalDate() },
        )
    }

    /** #13 — typed factory objects provide a reflection-free `mock(FooMockFactory)` DSL. */
    @Test
    fun `typed factory object creates explicit generated mocks`() {
        val formatter = typedExternalFormatterMock(ExternalFormatterMockFactory)
        val timeSource = typedTimeSourceMock(TimeSourceMockFactory)
        val clock = Clock.fixed(Instant.parse("2027-04-05T06:07:08Z"), ZoneOffset.UTC)
        formatter.everyFormat { value -> "factory:$value" }
        timeSource.everyClock { clock }

        assertAll(
            { assertEquals("factory:note", formatter.format("note")) },
            { assertSame(clock, timeSource.clock) },
            { formatter.verifyFormat("note") },
            { timeSource.verifyClock() },
        )
    }

    /** #13 — typed factory objects also create spies without runtime type lookup. */
    @Test
    fun `typed factory object creates explicit generated spies`() {
        val formatter = typedExternalFormatterSpy(
            ExternalFormatterMockFactory,
            ExternalFormatter { value -> "real:$value" },
        )

        assertAll(
            { assertEquals("real:note", formatter.format("note")) },
            { formatter.verifyFormat("note") },
        )
    }

    /** #13 — generated call logs are per-instance and safe under parallel test calls. */
    @Test
    fun `generated mock verification is parallel safe`() = runTest {
        val formatter = externalFormatterMock()
        formatter.everyFormat { value -> value }

        coroutineScope {
            repeat(200) {
                launch(Dispatchers.Default) {
                    check(formatter.format("parallel") == "parallel")
                }
            }
        }

        formatter.verifyFormat("parallel", times = 200)
    }
}
