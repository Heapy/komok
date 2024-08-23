package io.heapy.komok.server.core

import io.heapy.komok.tech.logging.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame

class WebSocketFrameHandler : SimpleChannelInboundHandler<WebSocketFrame>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        frame: WebSocketFrame
    ) {
        if (frame is TextWebSocketFrame) {
            val request = frame.text()
            println("Received: $request")
            ctx
                .channel()
                .writeAndFlush(TextWebSocketFrame(request.uppercase()))
        } else {
            throw UnsupportedOperationException("Unsupported frame type: ${frame.javaClass.name}")
        }
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable
    ) {
        cause.printStackTrace()
        ctx.close()
    }

    private companion object : Logger()
}
