package io.heapy.komok.server.core

import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class NettyServerModule(
    private val configurationModule: ConfigurationModule,
) {
    open val ioHandlerFactoryProvider: IoConfigurationProvider by lazy {
        SystemDependentIoConfigurationProvider()
    }

    open val config: NettyServerConfiguration by lazy {
        configurationModule
            .config
            .read(
                deserializer = DefaultNettyServerConfiguration.serializer(),
                path = "netty",
            )
    }

    open val rootHandler: HttpHandler by lazy<HttpHandler> {
        TODO("Implement root handler in module that requires netty server")
    }

    open val nettyServer: NettyServer by lazy {
        NettyServer(
            config = config,
            rootHandler = rootHandler,
            ioConfigurationProvider = ioHandlerFactoryProvider,
        )
    }
}
