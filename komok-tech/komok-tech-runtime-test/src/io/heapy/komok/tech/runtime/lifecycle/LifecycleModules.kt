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
open class LogModule {
    context(_: KomokContext)
    open suspend fun events(): MutableList<String> =
        CopyOnWriteArrayList()
}

@Module
open class ConfigLifecycleModule(
    private val log: LogModule,
) {
    @Startup
    context(_: KomokContext)
    open suspend fun start() {
        log.events().add("config.start")
    }

    @Shutdown
    context(_: KomokContext)
    open suspend fun stop() {
        log.events().add("config.stop")
    }
}

@Module
open class LifecycleModule(
    private val config: ConfigLifecycleModule,
    private val log: LogModule,
) {
    @Startup
    context(_: KomokContext)
    open suspend fun start() {
        log.events().add("app.start")
    }

    @Shutdown
    context(_: KomokContext)
    open suspend fun stop() {
        log.events().add("app.stop")
    }
}

/**
 * #18 sample: its `@Startup` hook fails after the dependency's hook has run, so the
 * generated graph must compensate (run the started modules' `@Shutdown` in reverse)
 * and fail fast.
 */
@Module
open class FailingBootModule(
    private val config: ConfigLifecycleModule,
    private val log: LogModule,
) {
    @Startup
    context(_: KomokContext)
    open suspend fun start() {
        log.events().add("failing.start")
        error("intentional boot failure")
    }

    @Shutdown
    context(_: KomokContext)
    open suspend fun stop() {
        log.events().add("failing.stop")
    }
}
