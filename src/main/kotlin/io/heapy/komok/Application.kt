@file:JvmName("Application")
package io.heapy.komok

import io.heapy.komok.business.ServerModule
import io.heapy.komok.infra.uptime.JvmUptime
import io.heapy.komok.logging.Logger
import io.heapy.komok.infra.uptime.JvmUptimeModule
import io.heapy.komok.tech.di.lib.Module
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.*

fun main() {
    createApplicationModule {}
        .komokApplication
        .run()
}

class KomokApplication(
    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>,
    private val jvmUptime: JvmUptime,
) {
    fun run() {
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
    open val komokApplication by lazy {
        KomokApplication(
            server = serverModule.server,
            jvmUptime = jvmUptimeModule.jvmUptime,
        )
    }
}
