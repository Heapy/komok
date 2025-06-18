package io.heapy.komok.tech.time

import io.heapy.komok.tech.di.lib.Module
import java.time.Clock

@Module
open class TimeSourceModule {
    open val timeSource: TimeSource by lazy {
        TimeSource(
            clock = Clock.systemDefaultZone(),
        )
    }
}
