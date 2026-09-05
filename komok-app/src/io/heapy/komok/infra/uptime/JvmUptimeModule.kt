package io.heapy.komok.infra.uptime

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class JvmUptimeModule {
    context(_: KomokContext)
    open suspend fun jvmUptime(): JvmUptime =
        DefaultJvmUptime()
}
