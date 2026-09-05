package io.heapy.komok.tech.time

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import java.time.Clock

@Module
interface TimeSourceModule {
    context(_: KomokContext)
    suspend fun timeSource(): TimeSource =
        TimeSource(
            clock = Clock.systemDefaultZone(),
        )
}
