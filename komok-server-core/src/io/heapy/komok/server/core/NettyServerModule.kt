package io.heapy.komok.server.core

import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class NettyServerModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KomokContext)
    open suspend fun ioHandlerFactoryProvider(): IoConfigurationProvider =
        SystemDependentIoConfigurationProvider()

    context(_: KomokContext)
    open suspend fun config(): NettyServerConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = DefaultNettyServerConfiguration.serializer(),
                path = "netty",
            )

    context(_: KomokContext)
    open suspend fun rootHandler(): HttpHandler =
        object : HttpHandler {
            override suspend fun handleRequest(exchange: NettyHttpServerExchange) {
                println("Calling handler")
                exchange.setResponseContent("Hello World")
                exchange.endExchange()
            }
        }

    context(_: KomokContext)
    open suspend fun nettyServer(): NettyServer =
        NettyServer(
            config = config(),
            rootHandler = rootHandler(),
            ioConfigurationProvider = ioHandlerFactoryProvider(),
        )
}
