package io.heapy.komok.tech.config.dotenv

import io.heapy.komok.tech.config.common.Environment
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class DotenvModule {
    context(_: KomokContext)
    suspend fun dotenv(): Environment =
        io.heapy.komok.tech.config.dotenv.dotenv()
}
