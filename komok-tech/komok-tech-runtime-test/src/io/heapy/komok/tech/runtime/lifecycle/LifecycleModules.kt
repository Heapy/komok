package io.heapy.komok.tech.runtime.lifecycle

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.runtime.Shutdown
import io.heapy.komok.tech.runtime.Startup
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lifecycle sample: `LifecycleModule(ConfigModule(LogModule), LogModule)`. The shared
 * `events` list is a binding, so hooks and the test observe the same recorded order.
 */

@Module
class LogModule {
    context(_: KomokContext)
    suspend fun events(): MutableList<String> =
        CopyOnWriteArrayList()
}

@Module
class ConfigLifecycleModule(
    private val log: LogModule,
) {
    @Startup
    context(_: KomokContext)
    suspend fun start() {
        log.events().add("config.start")
    }

    @Shutdown
    context(_: KomokContext)
    suspend fun stop() {
        log.events().add("config.stop")
    }
}

@Module
class LifecycleModule(
    private val config: ConfigLifecycleModule,
    private val log: LogModule,
) {
    @Startup
    context(_: KomokContext)
    suspend fun start() {
        log.events().add("app.start")
    }

    @Shutdown
    context(_: KomokContext)
    suspend fun stop() {
        log.events().add("app.stop")
    }
}

/**
 * #18 sample: its `@Startup` hook fails after the dependency's hook has run, so the
 * generated graph must compensate (run the started modules' `@Shutdown` in reverse)
 * and fail fast.
 */
@Module
class FailingBootModule(
    private val config: ConfigLifecycleModule,
    private val log: LogModule,
) {
    @Startup
    context(_: KomokContext)
    suspend fun start() {
        log.events().add("failing.start")
        error("intentional boot failure")
    }

    @Shutdown
    context(_: KomokContext)
    suspend fun stop() {
        log.events().add("failing.stop")
    }
}
