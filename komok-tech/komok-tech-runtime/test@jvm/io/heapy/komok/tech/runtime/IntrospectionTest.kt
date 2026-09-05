package io.heapy.komok.tech.runtime

import io.heapy.komok.tech.runtime.fixtures.ConfigModuleKeys
import io.heapy.komok.tech.runtime.fixtures.ConfigModule_Komok
import io.heapy.komok.tech.runtime.fixtures.JdbcModuleKeys
import io.heapy.komok.tech.runtime.fixtures.JdbcModule_Komok
import io.heapy.komok.tech.runtime.fixtures.ServiceModuleKeys
import io.heapy.komok.tech.runtime.fixtures.ServiceModule_Komok
import io.heapy.komok.tech.runtime.fixtures.UtilsModuleKeys
import io.heapy.komok.tech.runtime.fixtures.UtilsModule_Komok
import io.heapy.komok.tech.runtime.fixtures.fixedTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class IntrospectionTest {
    /** edges()/bindings() expose the recorded binding graph; isShared() reports sharing. */
    @Test
    fun `introspection - edges, bindings and isShared report the realized graph`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val config = ConfigModule_Komok(rt)
        val jdbc = JdbcModule_Komok(rt, config)
        val utils = UtilsModule_Komok(rt)
        val root = rt.rootHandle()

        val rootDataSource = root.read { jdbc.dataSource() }
        val rootClock = root.read { utils.clock() }

        val fork = root.fork(
            mapOf(UtilsModuleKeys.timeSource to BindingOverride.Value(fixedTimeSource())),
        )
        val forkClock = fork.read { utils.clock() }
        val forkDataSource = fork.read { jdbc.dataSource() }

        assertAll(
            { assertTrue(rt.edges().contains(Edge(JdbcModuleKeys.dataSource, ConfigModuleKeys.jdbcUrl))) },
            // isShared agrees with actual instance identity.
            { assertSame(rootDataSource, forkDataSource) },
            { assertNotSame(rootClock, forkClock) },
            {
                assertTrue(
                    rt.bindings().containsAll(
                        setOf(
                            JdbcModuleKeys.dataSource,
                            ConfigModuleKeys.jdbcUrl,
                            UtilsModuleKeys.clock,
                            UtilsModuleKeys.timeSource,
                        ),
                    ),
                )
            },
            // non-cone dataSource is shared by reference; cone clock/timeSource are not.
            { assertTrue(root.isShared(JdbcModuleKeys.dataSource, fork)) },
            { assertFalse(root.isShared(UtilsModuleKeys.clock, fork)) },
            { assertFalse(root.isShared(UtilsModuleKeys.timeSource, fork)) },
        )
        rt.close()
    }

    /** bindings() lists every realized binding, including ones with no recorded edges. */
    @Test
    fun `bindings - includes realized bindings without edges`() = runTest {
        val rt = KomokRuntime(dispatcher = Dispatchers.Default)
        val module = ServiceModule_Komok(rt)
        val handle = rt.rootHandle()

        val plain = handle.read { module.plain() }

        assertAll(
            { assertEquals("plain-value", plain) },
            { assertTrue(rt.bindings().contains(ServiceModuleKeys.plain)) },
            { assertTrue(rt.edges().isEmpty()) },
        )
        rt.close()
    }

    /** JSON metacharacters in names are escaped (backtick identifiers may contain them). */
    @Test
    fun `graph payload escapes json metacharacters`() {
        val payload = GraphPayload(
            modules = listOf(
                ModuleDescriptor("com.example.\"Q\"\nModule", emptyList(), emptyList()),
            ),
            edges = emptyList(),
        )

        assertEquals(
            "{\"modules\":[{\"fqn\":\"com.example.\\\"Q\\\"\\nModule\"," +
                "\"dependencies\":[],\"bindings\":[]}],\"edges\":[]}",
            payload.toJson(),
        )
    }

    /** The /graph payload serializes deterministically (sorted, no reflection). */
    @Test
    fun `graph payload serializes to deterministic json`() {
        val payload = GraphPayload(
            modules = listOf(
                ModuleDescriptor("com.example.B", emptyList(), listOf("com.example.B.x")),
                ModuleDescriptor("com.example.A", listOf("com.example.B"), listOf("com.example.A.y")),
            ),
            edges = listOf(EdgeDescriptor("com.example.A.y", "com.example.B.x")),
        )

        val json = payload.toJson()

        assertEquals(
            "{\"modules\":[" +
                "{\"fqn\":\"com.example.A\",\"dependencies\":[\"com.example.B\"],\"bindings\":[\"com.example.A.y\"]}," +
                "{\"fqn\":\"com.example.B\",\"dependencies\":[],\"bindings\":[\"com.example.B.x\"]}" +
                "],\"edges\":[" +
                "{\"from\":\"com.example.A.y\",\"to\":\"com.example.B.x\"}" +
                "]}",
            json,
        )
    }
}
