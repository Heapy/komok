package io.heapy.komok.business.entrypoint

import io.heapy.komok.infra.http.server.ServerModule
import io.heapy.komok.infra.startup_tasks.StartupTaskModule
import io.heapy.komok.infra.uptime.JvmUptimeModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class KomokApplicationModule(
    private val serverModule: ServerModule,
    private val jvmUptimeModule: JvmUptimeModule,
    private val startupTaskModule: StartupTaskModule,
) {
    open val komokApplication by lazy {
        KomokApplication(
            server = serverModule.server,
            jvmUptime = jvmUptimeModule.jvmUptime,
            startupTaskExecutor = startupTaskModule.startupTaskExecutor,
        )
    }
}
