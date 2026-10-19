package io.heapy.komok.infra.uptime

import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class JvmUptimeModule {
    context(_: KotomiContext)
    suspend fun jvmUptime(): JvmUptime =
        DefaultJvmUptime()
}
