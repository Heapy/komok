package io.heapy.komok.server.core

import io.heapy.kotomi.config.ConfigurationModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class NettyServerModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KotomiContext)
    suspend fun ioHandlerFactoryProvider(): IoConfigurationProvider =
        SystemDependentIoConfigurationProvider()

    context(_: KotomiContext)
    suspend fun config(): NettyServerConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = DefaultNettyServerConfiguration.serializer(),
                path = "netty",
            )

    context(_: KotomiContext)
    suspend fun rootHandler(): HttpHandler =
        object : HttpHandler {
            override suspend fun handleRequest(exchange: NettyHttpServerExchange) {
                println("Calling handler")
                exchange.setResponseContent("Hello World")
                exchange.endExchange()
            }
        }

    context(_: KotomiContext)
    suspend fun nettyServer(): NettyServer =
        NettyServer(
            config = config(),
            rootHandler = rootHandler(),
            ioConfigurationProvider = ioHandlerFactoryProvider(),
        )
}
