package io.heapy.komok.tech.runtime.audit

import io.heapy.komok.tech.runtime.test.wrapper.AuditTrail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class AuditWrapperGraphTest {
    /**
     * A ServiceLoader-discovered third-party wrapper decorates a service — custom
     * annotation, custom helper, custom context parameter — with zero changes to the
     * core processor.
     */
    @Test
    fun `custom wrapper plugin decorates through the spi`() = runTest {
        val trail = AuditTrail()
        val graph = createAuditModuleGraph { dispatcher = Dispatchers.Default }
        val service = graph.modules.auditModule.accountService()

        val result = withTrail(trail) { service.open("main") }

        assertAll(
            { assertEquals("account:main", result) },
            {
                assertEquals(
                    listOf("io.heapy.komok.tech.runtime.audit.AccountService.open"),
                    trail.entries,
                )
            },
        )
        graph.stop()
    }
}

/** Supplies the [AuditTrail] context argument positionally. */
private suspend fun <T> withTrail(
    trail: AuditTrail,
    block: suspend context(AuditTrail) () -> T,
): T = block(trail)
