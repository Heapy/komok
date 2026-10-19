package io.heapy.komok.infra.http.server

import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class ServerConfigurationModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KomokContext)
    suspend fun config(): ServerConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = ServerConfiguration.serializer(),
                path = "server",
            )
}
