package io.heapy.komok.tech.logging.jul

import io.heapy.komok.tech.logging.LoggingContext
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import java.util.logging.Logger

@Module
class KomokJulHandlerModule {
    context(_: KomokContext)
    suspend fun loggingContext(): LoggingContext =
        error("loggingContext must be provided")

    context(_: KomokContext)
    suspend fun julHandler(): KomokJulHandler =
        KomokJulHandler(loggingContext()).also { handler ->
            Logger.getLogger("").addHandler(handler)
        }
}
