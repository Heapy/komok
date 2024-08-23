package io.heapy.komok.infra.uptime

import io.heapy.komok.tech.di.lib.Module
import java.lang.management.ManagementFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface JvmUptime {
    fun uptime(): Duration
}

private class DefaultJvmUptime : JvmUptime {
    override fun uptime(): Duration {
        return ManagementFactory.getRuntimeMXBean()
            .uptime
            .milliseconds
    }
}

@Module
open class JvmUptimeModule {
    open val jvmUptime: JvmUptime by lazy {
        DefaultJvmUptime()
    }
}
