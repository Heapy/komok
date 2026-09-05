package io.heapy.komok.tech.runtime.sample

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.time.TimeSource
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

/**
 * E2E sample graph: `AppModule(UtilsModule, JdbcModule(ConfigModule), WorkerModule(ConfigModule))`.
 * `ConfigModule` is referenced by both `JdbcModule` and `WorkerModule`, so it is interned to
 * one module node. Classes are explicitly `open` (the all-open plugin is a separate UX layer).
 */

class Db(val url: String, val poolSize: Int)
class Worker(val url: String)
class Application(val db: Db, val worker: Worker, val clock: Clock)

@Module
open class ConfigModule {
    val jdbcUrlCalls = AtomicInteger(0)

    context(_: KomokContext)
    open suspend fun jdbcUrl(): String {
        jdbcUrlCalls.incrementAndGet()
        return "jdbc:postgresql://localhost/db"
    }

    context(_: KomokContext)
    open suspend fun poolSize(): Int = 10
}

@Module
open class UtilsModule {
    val timeSourceCalls = AtomicInteger(0)

    context(_: KomokContext)
    open suspend fun timeSource(): TimeSource {
        timeSourceCalls.incrementAndGet()
        return TimeSource(Clock.systemUTC())
    }

    context(_: KomokContext)
    open suspend fun clock(): Clock =
        timeSource().clock
}

@Module
open class JdbcModule(
    private val config: ConfigModule,
) {
    context(_: KomokContext)
    open suspend fun dataSource(): Db =
        Db(config.jdbcUrl(), config.poolSize())
}

@Module
open class WorkerModule(
    private val config: ConfigModule,
) {
    context(_: KomokContext)
    open suspend fun worker(): Worker =
        Worker(config.jdbcUrl())
}

@Module
open class AppModule(
    private val utils: UtilsModule,
    private val jdbc: JdbcModule,
    private val worker: WorkerModule,
) {
    context(_: KomokContext)
    open suspend fun application(): Application =
        Application(
            db = jdbc.dataSource(),
            worker = worker.worker(),
            clock = utils.clock(),
        )
}
