package io.heapy.komok.server.core

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil

class HttpClient {

    fun sendGetRequest(
        host: String,
        port: Int,
        uri: String,
    ) {
        val group: EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())

        try {
            val b = Bootstrap()
            b
                .group(group)
                .channel(NioSocketChannel::class.java)
                .handler(
                    object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        override fun initChannel(ch: SocketChannel) {
                            val p: ChannelPipeline = ch.pipeline()
                            p.addLast(HttpClientCodec())
                            p.addLast(HttpObjectAggregator(8192))
                            p.addLast(HttpResponseHandler())
                        }
                    },
                )

            val ch = b
                .connect(
                    host,
                    port,
                )
                .sync()
                .channel()

            val request = DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                uri,
                Unpooled.EMPTY_BUFFER,
            )
            request
                .headers()
                .set(
                    HttpHeaderNames.HOST,
                    host,
                )
            request
                .headers()
                .set(
                    HttpHeaderNames.CONNECTION,
                    HttpHeaderValues.CLOSE,
                )
            request
                .headers()
                .set(
                    HttpHeaderNames.ACCEPT_ENCODING,
                    HttpHeaderValues.GZIP,
                )

            ch.writeAndFlush(request)
            ch
                .closeFuture()
                .sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}

class HttpResponseHandler : SimpleChannelInboundHandler<FullHttpResponse>() {

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        response: FullHttpResponse,
    ) {
        val content = response
            .content()
            .toString(CharsetUtil.UTF_8)
        println("Response received: $content")
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        cause.printStackTrace()
        ctx.close()
    }
}

fun main() {
    val client = HttpClient()
    client.sendGetRequest(
        "localhost",
        8080,
        "/",
    )
}
