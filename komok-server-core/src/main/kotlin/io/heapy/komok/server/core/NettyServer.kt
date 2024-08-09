package io.heapy.komok.server.core

import io.heapy.komok.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.stream.ChunkedWriteHandler
import java.net.InetSocketAddress

class NettyServer(
    private val config: NettyServerConfiguration,
    private val ioConfigurationProvider: IoConfigurationProvider,
    private val rootHandler: HttpHandler,
) {
    fun start() {
        val factoryProvider = ioConfigurationProvider()
        val bossGroup = MultiThreadIoEventLoopGroup(
            1,
            factoryProvider.ioHandlerFactory,
        )
        val workerGroup = MultiThreadIoEventLoopGroup(
            factoryProvider.ioHandlerFactory,
        )

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap
                .group(
                    bossGroup,
                    workerGroup,
                )
                .channel(factoryProvider.serverSocketChannel.java)
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            val channelPipeline = ch.pipeline()
                            channelPipeline.addLast(HttpServerCodec())
                            channelPipeline.addLast(HttpServerKeepAliveHandler())
                            channelPipeline.addLast(HttpObjectAggregator(64 * 1024))
                            channelPipeline.addLast(ChunkedWriteHandler())
                            channelPipeline.addLast(HttpRequestHandler(rootHandler))
                            channelPipeline.addLast(WebSocketServerProtocolHandler("/ws"))
                            channelPipeline.addLast(WebSocketFrameHandler())
                        }
                    },
                )
                .option(
                    ChannelOption.SO_BACKLOG,
                    config.backlog,
                )
                .childOption(
                    ChannelOption.SO_KEEPALIVE,
                    config.keepAlive,
                )

            val channelFuture = serverBootstrap
                .bind(config.port)
                .sync()

            val actualPort = channelFuture
                .channel()
                .localAddress() as InetSocketAddress

            log.info("Server listening on {}", actualPort)

            channelFuture
                .channel()
                .closeFuture()
                .sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }

    private companion object : Logger()
}

fun main() {
    val module = createNettyServerModule {
        config {
            object : NettyServerConfiguration {
                override val port = 8080
                override val shutdownTimeout = 1000
                override val keepAlive = true
                override val backlog = 128
            }
        }
        rootHandler {
            HttpHandler { request ->
                println(request.method)
            }
        }
    }

    module.nettyServer.start()
}
