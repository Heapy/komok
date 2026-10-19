package io.heapy.komok.infra.http.server

import io.heapy.kotomi.config.ConfigurationModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class ServerConfigurationModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KotomiContext)
    suspend fun config(): ServerConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = ServerConfiguration.serializer(),
                path = "server",
            )
}
