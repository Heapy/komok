package io.heapy.komok.tech.di.delegate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
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

    @Test
    fun `mutually dependent beans read from two threads fail on both threads`() {
        val startedFirst = CountDownLatch(1)
        val startedSecond = CountDownLatch(1)
        lateinit var first: MutableBean<String>
        lateinit var second: MutableBean<String>

        first = MutableBean(
            initializer = {
                startedFirst.countDown()
                check(startedSecond.await(3, TimeUnit.SECONDS))
                "first-${second.value}"
            },
            name = "test.first",
        )
        second = MutableBean(
            initializer = {
                startedSecond.countDown()
                check(startedFirst.await(3, TimeUnit.SECONDS))
                "second-${first.value}"
            },
            name = "test.second",
        )
        val executor = daemonExecutor()

        try {
            val a = executor.submit<String> { first.value }
            val b = executor.submit<String> { second.value }

            assertAll(
                { assertReportsCycle { a.get(6, TimeUnit.SECONDS) } },
                { assertReportsCycle { b.get(6, TimeUnit.SECONDS) } },
            )
        } finally {
            val _ = executor.shutdownNow()
        }
    }
}

private fun daemonExecutor(): ExecutorService =
    Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable).also { it.isDaemon = true }
    }

private fun assertReportsCycle(read: () -> Any?) {
    val cause = assertThrows<ExecutionException> { val _ = read() }.cause
    val message = cause?.message.orEmpty()

    assertTrue(
        message.contains("cycle across threads") || message.contains("reads itself"),
        "unexpected cause: $cause",
    )
}
