package io.heapy.komok.business

import io.heapy.komok.configuration.ConfigModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class ServerConfigurationModule(
    private val configModule: ConfigModule,
) {
    open val config: ServerConfiguration by lazy {
        configModule.config.read(
            deserializer = ServerConfiguration.serializer(),
            path = "server",
        )
    }
}
