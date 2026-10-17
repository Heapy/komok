package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThrowableProxyTest {

    @Test
    fun `simple exception is captured`() {
        val exception = RuntimeException("test error")
        val proxy = ThrowableProxy.create(exception)

        assertEquals("java.lang.RuntimeException", proxy.className)
        assertEquals("test error", proxy.message)
        assertNull(proxy.cause)
        assertTrue(proxy.stackTrace.isNotEmpty())
        assertEquals(0, proxy.suppressed.size)
    }

    @Test
    fun `chained exception captures cause`() {
        val original = IllegalArgumentException("bad arg")
        val exception = RuntimeException("wrapper", original)
        val proxy = ThrowableProxy.create(exception)

        assertEquals("java.lang.RuntimeException", proxy.className)
        val causeProxy = proxy.cause
        assertNotNull(causeProxy)
        assertEquals("java.lang.IllegalArgumentException", causeProxy!!.className)
        assertEquals("bad arg", causeProxy.message)
    }

    @Test
    fun `common frames are computed`() {
        val cause = IllegalArgumentException("bad arg")
        val exception = RuntimeException("wrapper", cause)
        val proxy = ThrowableProxy.create(exception)

        assertTrue(proxy.cause!!.commonFramesCount >= 0)
    }

    @Test
    fun `suppressed exceptions are captured`() {
        val exception = RuntimeException("main")
        exception.addSuppressed(IllegalStateException("suppressed1"))
        exception.addSuppressed(IllegalArgumentException("suppressed2"))

        val proxy = ThrowableProxy.create(exception)

        assertEquals(2, proxy.suppressed.size)
        assertEquals("java.lang.IllegalStateException", proxy.suppressed[0].className)
        assertEquals("java.lang.IllegalArgumentException", proxy.suppressed[1].className)
    }

    @Test
    fun `toString produces readable output`() {
        val cause = IllegalArgumentException("bad arg")
        val exception = RuntimeException("wrapper", cause)
        val proxy = ThrowableProxy.create(exception)

        val str = proxy.toString()
        assertTrue(str.contains("java.lang.RuntimeException: wrapper"))
        assertTrue(str.contains("Caused by:"))
        assertTrue(str.contains("java.lang.IllegalArgumentException: bad arg"))
    }

    @Test
    fun `null message is handled`() {
        val exception = RuntimeException()
        val proxy = ThrowableProxy.create(exception)

        assertEquals("java.lang.RuntimeException", proxy.className)
        assertNull(proxy.message)
    }

    @Test
    fun `stack trace elements are proxied`() {
        val exception = RuntimeException("test")
        val proxy = ThrowableProxy.create(exception)

        assertTrue(proxy.stackTrace.isNotEmpty())
        val firstFrame = proxy.stackTrace[0]
        assertTrue(firstFrame.string.isNotEmpty())
        assertEquals(firstFrame.stackTraceElement, exception.stackTrace[0])
    }
}
