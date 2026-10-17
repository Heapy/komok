package io.heapy.komok.tech.di.delegate

import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertNotEquals

@OptIn(ObsoleteWorkersApi::class)
class ThreadTokenTest {
    @Test
    fun `each thread gets its own token`() {
        val worker = Worker.start()

        try {
            val other = worker
                .execute(TransferMode.SAFE, { }) { currentThreadToken() }
                .result

            assertNotEquals(currentThreadToken(), other)
        } finally {
            worker.requestTermination().result
        }
    }
}
