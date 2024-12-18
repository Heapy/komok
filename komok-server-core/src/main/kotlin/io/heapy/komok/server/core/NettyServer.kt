package io.heapy.komok.server.core

import io.heapy.komok.tech.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import io.netty.handler.stream.ChunkedWriteHandler
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NettyServer(
    private val config: NettyServerConfiguration,
    private val ioConfigurationProvider: IoConfigurationProvider,
    private val rootHandler: HttpHandler,
) {
    fun start(): StartedServer {
        val factoryProvider = ioConfigurationProvider()
        val parentGroup = MultiThreadIoEventLoopGroup(
            1,
            factoryProvider.ioHandlerFactory,
        )
        val childGroup = MultiThreadIoEventLoopGroup(
            factoryProvider.ioHandlerFactory,
        )

        val serverBootstrap = ServerBootstrap()
        serverBootstrap
            .group(
                parentGroup,
                childGroup,
            )
            .channel(factoryProvider.serverSocketChannel)
            .childHandler(
                object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        val channelPipeline = ch.pipeline()
                        channelPipeline.addLast(HttpServerCodec())
                        channelPipeline.addLast(HttpServerKeepAliveHandler())
                        channelPipeline.addLast(HttpObjectAggregator(64 * 1024))
                        channelPipeline.addLast(ChunkedWriteHandler())
                        channelPipeline.addLast(HttpRequestHandler(rootHandler))
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
            .channel()

        val actualPort = channelFuture
            .localAddress() as InetSocketAddress

        log.info("Server listening on {}", actualPort)

        return StartedServer(
            closeFuture = channelFuture.closeFuture(),
            port = actualPort,
            bossGroup = parentGroup,
            workerGroup = childGroup,
        )
    }

    private companion object : Logger()
}

class StoppedServer(
    val port: InetSocketAddress,
)

class StartedServer(
    val port: InetSocketAddress,
    private val bossGroup: MultiThreadIoEventLoopGroup,
    private val workerGroup: MultiThreadIoEventLoopGroup,
    private val closeFuture: ChannelFuture,
) {
    fun await() {
        closeFuture.sync()
    }

    fun stop(
        quitePeriod: Duration = 2.seconds,
        timeout: Duration = 5.seconds,
    ): StoppedServer {
        closeFuture.cancel(false)

        bossGroup.shutdownGracefully(
            quitePeriod.inWholeMilliseconds,
            timeout.inWholeMilliseconds,
            TimeUnit.MILLISECONDS,
        ).sync()

        workerGroup.shutdownGracefully(
            quitePeriod.inWholeMilliseconds,
            timeout.inWholeMilliseconds,
            TimeUnit.MILLISECONDS,
        ).sync()

        return StoppedServer(port)
    }
}

class CreateUserHandler : HttpHandler {
    override suspend fun handleRequest(exchange: NettyHttpServerExchange) {
        val requestBody = exchange.getRequestBody()
        exchange.setStatusCode(201)
        exchange.setResponseContent(requestBody)
        exchange.getResponseHeaders()[HttpHeaderNames.CONTENT_TYPE] = "application/json; charset=UTF-8"
        exchange.endExchange()
    }
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
            CreateUserHandler()
        }
    }

    module.nettyServer.start()
}
