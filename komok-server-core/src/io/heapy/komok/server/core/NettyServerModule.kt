package io.heapy.komok.server.core

import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class NettyServerModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KomokContext)
    suspend fun ioHandlerFactoryProvider(): IoConfigurationProvider =
        SystemDependentIoConfigurationProvider()

    context(_: KomokContext)
    suspend fun config(): NettyServerConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = DefaultNettyServerConfiguration.serializer(),
                path = "netty",
            )

    context(_: KomokContext)
    suspend fun rootHandler(): HttpHandler =
        object : HttpHandler {
            override suspend fun handleRequest(exchange: NettyHttpServerExchange) {
                println("Calling handler")
                exchange.setResponseContent("Hello World")
                exchange.endExchange()
            }
        }

    context(_: KomokContext)
    suspend fun nettyServer(): NettyServer =
        NettyServer(
            config = config(),
            rootHandler = rootHandler(),
            ioConfigurationProvider = ioHandlerFactoryProvider(),
        )
}
