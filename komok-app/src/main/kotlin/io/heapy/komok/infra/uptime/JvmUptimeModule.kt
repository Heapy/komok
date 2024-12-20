package io.heapy.komok.infra.uptime

import io.heapy.komok.tech.di.lib.Module

@Module
open class JvmUptimeModule {
    open val jvmUptime: JvmUptime by lazy {
        DefaultJvmUptime()
    }
}
