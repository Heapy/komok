package io.heapy.komok.server.core

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.AttributeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HttpRequestHandler(
    private val rootHandler: HttpHandler,
) : SimpleChannelInboundHandler<FullHttpRequest>() {
    private val websocketPath = "/ws"

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        // Attach a new Job to the context
        val job = Job()
        ctx
            .channel()
            .attr(ATTRIBUTE_KEY_JOB)
            .set(job)
    }

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        request: FullHttpRequest
    ) {
        if (websocketPath.equals(
                request.uri(),
                ignoreCase = true
            )
        ) {
            ctx.fireChannelRead(request.retain())
        } else {
            CoroutineScope(Dispatchers.Default + ctx.channel().attr(ATTRIBUTE_KEY_JOB).get())
                .launch {
                    val exchange = object : HttpExchange {
                        override val method: Method by lazy(LazyThreadSafetyMode.PUBLICATION) {
                            when (val method = request.method()) {
                                HttpMethod.DELETE -> Method.DELETE
                                HttpMethod.GET -> Method.GET
                                HttpMethod.HEAD -> Method.HEAD
                                HttpMethod.OPTIONS -> Method.OPTIONS
                                HttpMethod.PATCH -> Method.PATCH
                                HttpMethod.POST -> Method.POST
                                HttpMethod.PUT -> Method.PUT
                                HttpMethod.TRACE -> Method.TRACE
                                else -> error("Unsupported method: $method")
                            }
                        }
                    }

                    rootHandler.handle(exchange)
                }

            val content = "Welcome to the Netty HTTP Server".toByteArray()
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content)
            )
            response
                .headers()
                .set(
                    HttpHeaderNames.CONTENT_TYPE,
                    "text/plain"
                )
            response
                .headers()
                .set(
                    HttpHeaderNames.CONTENT_LENGTH,
                    response
                        .content()
                        .readableBytes()
                )

            ctx
                .writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE)
        }
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable
    ) {
        cause.printStackTrace()
        ctx.close()
    }

    private companion object {
        private val ATTRIBUTE_KEY_JOB: AttributeKey<Job> = AttributeKey.newInstance("CoroutineJob")
    }
}
