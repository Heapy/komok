package io.heapy.komok.tech.logging.jul

import io.heapy.komok.tech.di.lib.Module
import io.heapy.komok.tech.logging.LoggingContext
import java.util.logging.Logger

@Module
open class KomokJulHandlerModule {
    open val loggingContext: LoggingContext by lazy {
        error("loggingContext must be provided")
    }

    open val julHandler: KomokJulHandler by lazy {
        KomokJulHandler(loggingContext).also { handler ->
            Logger.getLogger("").addHandler(handler)
        }
    }
}