package io.heapy.komok.infra.uptime

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class JvmUptimeModule {
    context(_: KomokContext)
    suspend fun jvmUptime(): JvmUptime =
        DefaultJvmUptime()
}
