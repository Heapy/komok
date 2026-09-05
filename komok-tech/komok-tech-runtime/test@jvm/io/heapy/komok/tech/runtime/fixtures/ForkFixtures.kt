package io.heapy.komok.tech.runtime.fixtures

import io.heapy.komok.tech.runtime.BindingId
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.KomokRuntime
import io.heapy.komok.tech.runtime.ProviderSignature
import io.heapy.komok.tech.runtime.currentKomokContext
import io.heapy.komok.tech.time.TimeSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hand-wired cross-module fixtures for P2 fork verification. Module interning is
 * simulated by hand: one [ConfigModule_Komok] instance is passed to two dependents, so
 * both share one [BindingId].
 */

class FakeDataSource(val url: String)
class FakeWorker(val url: String)

fun fixedTimeSource(): TimeSource =
    TimeSource(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

private const val CONFIG_FQN = "io.heapy.komok.tech.runtime.fixtures.ConfigModule"

object ConfigModuleKeys {
    val jdbcUrl = BindingId(CONFIG_FQN, ProviderSignature("jdbcUrl"))
}

open class ConfigModule {
    val jdbcUrlCalls = AtomicInteger(0)

    context(_: KomokContext)
    open suspend fun jdbcUrl(): String {
        jdbcUrlCalls.incrementAndGet()
        return "jdbc:postgresql://localhost/db"
    }
}

class ConfigModule_Komok(
    private val rt: KomokRuntime,
) : ConfigModule() {
    context(_: KomokContext)
    override suspend fun jdbcUrl(): String =
        rt.binding(ConfigModuleKeys.jdbcUrl) { super.jdbcUrl() }
}

private const val JDBC_FQN = "io.heapy.komok.tech.runtime.fixtures.JdbcModule"

object JdbcModuleKeys {
    val dataSource = BindingId(JDBC_FQN, ProviderSignature("dataSource"))
}

open class JdbcModule(
    private val config: ConfigModule,
) {
    context(_: KomokContext)
    open suspend fun dataSource(): FakeDataSource =
        FakeDataSource(config.jdbcUrl())
}

class JdbcModule_Komok(
    private val rt: KomokRuntime,
    config: ConfigModule,
) : JdbcModule(config) {
    context(_: KomokContext)
    override suspend fun dataSource(): FakeDataSource =
        rt.binding(JdbcModuleKeys.dataSource) { super.dataSource() }
}

private const val WORKER_FQN = "io.heapy.komok.tech.runtime.fixtures.WorkerModule"

object WorkerModuleKeys {
    val worker = BindingId(WORKER_FQN, ProviderSignature("worker"))
}

open class WorkerModule(
    private val config: ConfigModule,
) {
    context(_: KomokContext)
    open suspend fun worker(): FakeWorker =
        FakeWorker(config.jdbcUrl())
}

class WorkerModule_Komok(
    private val rt: KomokRuntime,
    config: ConfigModule,
) : WorkerModule(config) {
    context(_: KomokContext)
    override suspend fun worker(): FakeWorker =
        rt.binding(WorkerModuleKeys.worker) { super.worker() }
}

/**
 * #10 known-bad sample — a service that captures [KomokContext] for late graph reads,
 * violating "No late graph-read" (invariant #6): dependencies must be materialized
 * inside the provider body. Kept compiling on purpose; v1 has no automatic diagnosis
 * (the structural enforcement is deferred to Kotlin Local Lifetimes), so the test
 * documents the silently-wrong behavior the violation produces.
 */
class LateReadService(
    private val capturedContext: KomokContext,
    private val config: ConfigModule,
) {
    suspend fun url(): String {
        val read: suspend context(KomokContext) () -> String = { config.jdbcUrl() }
        // positional context-argument invocation — the same mechanism the runtime uses.
        return read(capturedContext)
    }
}

private const val LATE_READ_FQN = "io.heapy.komok.tech.runtime.fixtures.LateReadModule"

object LateReadModuleKeys {
    val lateService = BindingId(LATE_READ_FQN, ProviderSignature("lateService"))
}

open class LateReadModule(
    private val config: ConfigModule,
) {
    context(_: KomokContext)
    open suspend fun lateService(): LateReadService =
        LateReadService(currentKomokContext(), config)
}

class LateReadModule_Komok(
    private val rt: KomokRuntime,
    config: ConfigModule,
) : LateReadModule(config) {
    context(_: KomokContext)
    override suspend fun lateService(): LateReadService =
        rt.binding(LateReadModuleKeys.lateService) { super.lateService() }
}
