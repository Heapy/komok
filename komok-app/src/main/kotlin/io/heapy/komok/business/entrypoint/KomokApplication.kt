package io.heapy.komok.business.entrypoint

import io.heapy.komok.infra.startup_tasks.StartupTaskExecutor
import io.heapy.komok.infra.uptime.JvmUptime
import io.heapy.komok.tech.logging.Logger
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer

class KomokApplication(
    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>,
    private val jvmUptime: JvmUptime,
    private val startupTaskExecutor: StartupTaskExecutor,
) {
    fun run() {
        val uptime = jvmUptime.uptime()
        log.info("Application started. JVM running for $uptime")
        startupTaskExecutor.executeAll()
        server.start(wait = true)
    }

    private companion object : Logger()
}
