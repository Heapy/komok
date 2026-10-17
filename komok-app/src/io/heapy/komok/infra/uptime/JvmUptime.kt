package io.heapy.komok.infra.uptime

import java.lang.management.ManagementFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface JvmUptime {
    fun uptime(): Duration
}

internal class DefaultJvmUptime : JvmUptime {
    override fun uptime(): Duration {
        return ManagementFactory.getRuntimeMXBean()
            .uptime
            .milliseconds
    }
}
