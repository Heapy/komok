package io.heapy.komok.tech.runtime

import io.heapy.komok.tech.runtime.fixtures.ConfigModuleKeys
import io.heapy.komok.tech.runtime.fixtures.ConfigModule_Komok
import io.heapy.komok.tech.runtime.fixtures.JdbcModule_Komok
import io.heapy.komok.tech.runtime.fixtures.LateReadModule_Komok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * #10 — "No late graph-read": a provider must materialize all of its dependencies inside
 * its body; service values must not retain `KomokContext`/module refs for later reads.
 * v1 has no automatic diagnosis (discipline + review; structural enforcement deferred to
 * Kotlin Local Lifetimes), so the negative sample is pinned here as a known-bad fixture
 * demonstrating exactly how it goes wrong: the offender is shared into a fork as a
 * non-cone value (its late edge did not exist at fork time) and keeps reading the realm
 * it was realized in, bypassing the fork's overrides.
 */
class LateGraphReadTest {
    /** Positive sample: dependencies materialized in the provider body follow the fork. */
    @Test
    fun `positive sample - materialized dependencies respect the fork realm`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val root = rt.rootHandle()

        val rootDataSource = root.read { jdbc.dataSource() }
        val fork = root.fork(
            mapOf(ConfigModuleKeys.jdbcUrl to BindingOverride.Value("jdbc:forked")),
        )
        val forkDataSource = fork.read { jdbc.dataSource() }

        assertAll(
            { assertEquals("jdbc:postgresql://localhost/db", rootDataSource.url) },
            { assertEquals("jdbc:forked", forkDataSource.url) },
        )
        rt.close()
    }

    /** Known-bad sample: a captured KomokContext bypasses the fork realm (anti-pattern). */
    @Test
    fun `negative sample - captured context keeps reading the original realm`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val late = LateReadModule_Komok(rt, config)
        val root = rt.rootHandle()

        // the service captures the ROOT context during realization; no nested read
        // happened yet, so no lateService -> jdbcUrl edge exists at fork time.
        val service = root.read { late.lateService() }
        val fork = root.fork(
            mapOf(ConfigModuleKeys.jdbcUrl to BindingOverride.Value("jdbc:forked")),
        )
        val forkService = fork.read { late.lateService() }
        val lateUrl = forkService.url()
        val properUrl = fork.read { config.jdbcUrl() }

        assertAll(
            // outside the (empty) cone, the offender is shared into the fork by reference…
            { assertSame(service, forkService) },
            // …and its late read silently bypasses the fork override. This is the
            // documented wrong behavior of the anti-pattern, not desired semantics.
            { assertEquals("jdbc:postgresql://localhost/db", lateUrl) },
            { assertEquals("jdbc:forked", properUrl) },
        )
        rt.close()
    }
}
