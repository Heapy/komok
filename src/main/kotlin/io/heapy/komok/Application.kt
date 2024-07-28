@file:JvmName("Application")
package io.heapy.komok

import io.heapy.komok.business.ServerModule
import io.heapy.komok.infra.uptime.JvmUptime
import io.heapy.komok.infra.di.EntryPoint
import io.heapy.komok.infra.logging.Logger
import io.heapy.komok.infra.uptime.JvmUptimeModule
import io.heapy.komok.tech.di.lib.Module
import io.ktor.server.engine.*

suspend fun main() {
    createApplicationModule {}
        .komokServer
        .run()
}

class KomokServer(
    private val server: ApplicationEngine,
    private val jvmUptime: JvmUptime,
) : EntryPoint<Unit> {
    override suspend fun run() {
        val uptime = jvmUptime.uptime()
        log.info("Application started. JVM running for $uptime")
        server.start(wait = true)
    }

    private companion object : Logger()
}

@Module
open class ApplicationModule(
    private val serverModule: ServerModule,

    private val jvmUptimeModule: JvmUptimeModule,
) {
    open val komokServer by lazy {
        KomokServer(
            server = serverModule.server,
            jvmUptime = jvmUptimeModule.jvmUptime,
        )
    }
}
