package io.heapy.komok.infra.http.server

import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class ServerConfigurationModule(
    private val configurationModule: ConfigurationModule,
) {
    open val config: ServerConfiguration by lazy {
        configurationModule
            .config
            .read(
                deserializer = ServerConfiguration.serializer(),
                path = "server",
            )
    }
}
