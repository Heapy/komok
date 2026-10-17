package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MdcTest {

    private object RequestId : Mdc.Key<String>("requestId")
    private object UserId : Mdc.Key<Long>("userId")
    private object TenantId : Mdc.Key<String>("tenantId")

    @Test
    fun `empty MDC has zero size`() {
        val mdc = Mdc.Empty
        assertEquals(0, mdc.size)
        assertTrue(mdc.isEmpty)
    }

    @Test
    fun `get returns null for missing key`() {
        assertNull(Mdc.Empty[RequestId])
    }

    @Test
    fun `of creates single-entry MDC`() {
        val mdc = Mdc.of(RequestId, "abc-123")
        assertEquals("abc-123", mdc[RequestId])
        assertEquals(1, mdc.size)
    }

    @Test
    fun `with adds entry`() {
        val mdc = Mdc.Empty
            .with(RequestId, "req-1")
            .with(UserId, 42L)

        assertEquals("req-1", mdc[RequestId])
        assertEquals(42L, mdc[UserId])
        assertEquals(2, mdc.size)
    }

    @Test
    fun `with replaces existing entry`() {
        val mdc = Mdc.of(RequestId, "old")
            .with(RequestId, "new")

        assertEquals("new", mdc[RequestId])
        assertEquals(1, mdc.size)
    }

    @Test
    fun `plus merges two MDCs`() {
        val left = Mdc.of(RequestId, "req-1")
        val right = Mdc.of(UserId, 42L)
        val merged = left + right

        assertEquals("req-1", merged[RequestId])
        assertEquals(42L, merged[UserId])
        assertEquals(2, merged.size)
    }

    @Test
    fun `plus right side wins on collision`() {
        val left = Mdc.of(RequestId, "old")
        val right = Mdc.of(RequestId, "new")
        val merged = left + right

        assertEquals("new", merged[RequestId])
    }

    @Test
    fun `plus with empty is identity`() {
        val mdc = Mdc.of(RequestId, "req-1")

        val leftEmpty = Mdc.Empty + mdc
        val rightEmpty = mdc + Mdc.Empty

        assertEquals(mdc, leftEmpty)
        assertEquals(mdc, rightEmpty)
    }

    @Test
    fun `minus removes entry`() {
        val mdc = Mdc.of(RequestId, "req-1")
            .with(UserId, 42L)
            .minus(RequestId)

        assertNull(mdc[RequestId])
        assertEquals(42L, mdc[UserId])
        assertEquals(1, mdc.size)
    }

    @Test
    fun `entries returns name to value map`() {
        val mdc = Mdc.of(RequestId, "req-1")
            .with(UserId, 42L)

        val entries = mdc.entries()

        assertEquals(
            mapOf("requestId" to "req-1" as Any, "userId" to 42L as Any),
            entries,
        )
    }

    @Test
    fun `vararg of creates multi-entry MDC`() {
        val mdc = Mdc.of(
            RequestId to "req-1",
            UserId to 42L,
            TenantId to "acme",
        )

        assertEquals("req-1", mdc[RequestId])
        assertEquals(42L, mdc[UserId])
        assertEquals("acme", mdc[TenantId])
        assertEquals(3, mdc.size)
    }

    @Test
    fun `buildMdc DSL works`() {
        val mdc = buildMdc {
            RequestId to "req-1"
            UserId to 42L
        }

        assertEquals("req-1", mdc[RequestId])
        assertEquals(42L, mdc[UserId])
    }

    @Test
    fun `buildMdc invoke operator works`() {
        val mdc = buildMdc {
            RequestId("req-1")
            UserId(42L)
        }

        assertEquals("req-1", mdc[RequestId])
        assertEquals(42L, mdc[UserId])
    }

    @Test
    fun `toString is readable`() {
        val mdc = Mdc.of(RequestId, "req-1")
        val str = mdc.toString()
        assertTrue(str.contains("requestId=req-1"))
    }
}
