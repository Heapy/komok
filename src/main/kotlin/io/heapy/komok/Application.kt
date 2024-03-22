@file:JvmName("Application")
package io.heapy.komok

import io.heapy.komok.infra.di.EntryPoint
import io.heapy.komok.infra.di.komok
import io.heapy.komok.infra.di.provide
import io.heapy.komok.infra.logging.logger
import io.heapy.komok.business.serverModule
import io.ktor.server.engine.*
import java.lang.management.ManagementFactory

suspend fun main() {
    komok<RunServer> {
        dependency(serverModule)
        provide(::RunServer)
    }
}

class RunServer(
    private val server: ApplicationEngine,
) : EntryPoint<Unit> {
    override suspend fun run() {
        log.info("Application started. JVM running for ${uptime}ms")
        server.start(wait = true)
    }

    private companion object {
        private val log = logger {}
    }
}

private inline val uptime: Long
    get() = ManagementFactory.getRuntimeMXBean().uptime
