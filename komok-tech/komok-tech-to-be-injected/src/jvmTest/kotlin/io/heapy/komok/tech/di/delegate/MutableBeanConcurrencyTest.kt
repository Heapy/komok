package io.heapy.komok.tech.di.delegate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MutableBeanConcurrencyTest {
    @Test
    fun `concurrent readers initialize once`() {
        val initializationCount = AtomicInteger()
        val bean = MutableBean(
            initializer = {
                val _ = initializationCount.incrementAndGet()
                "value"
            },
            name = "test.bean",
        )
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        try {
            val futures = List(64) {
                executor.submit<String> {
                    start.await()
                    bean.value
                }
            }
            start.countDown()

            assertEquals(List(64) { "value" }, futures.map { it.get() })
            assertEquals(1, initializationCount.get())
        } finally {
            val _ = executor.shutdownNow()
        }
    }

    @Test
    fun `override during initialization is rejected`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val bean = MutableBean(
            initializer = {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                "value"
            },
            name = "test.bean",
        )
        val executor = Executors.newSingleThreadExecutor()

        try {
            val reader = executor.submit<String> { bean.value }
            assertTrue(started.await(5, TimeUnit.SECONDS))

            val exception = assertThrows<IllegalStateException> {
                bean.setValue("override")
            }
            assertTrue(exception.message!!.contains("already initializing"))

            release.countDown()
            assertEquals("value", reader.get())
        } finally {
            release.countDown()
            val _ = executor.shutdownNow()
        }
    }
}
