package io.heapy.komok.server.core

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.AttributeKey
import io.netty.util.concurrent.EventExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class NettyDispatcher(
    private val executor: EventExecutor,
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.execute(block)
    }
}

class HttpRequestHandler(
    private val rootHandler: HttpHandler,
) : SimpleChannelInboundHandler<FullHttpRequest>(false) {
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
        val exchange = NettyHttpServerExchange(ctx, request)
        val dispatcher = NettyDispatcher(ctx.executor())

        println("Channel read")
        CoroutineScope(dispatcher + ctx.channel().attr(ATTRIBUTE_KEY_JOB).get())
            .launch {
                try {
                    rootHandler.handleRequest(exchange)
                } catch (e: Exception) {
                    e.printStackTrace()
                    exchange.setStatusCode(500)
                    exchange.setResponseContent("Internal Server Error")
                    exchange.getResponseHeaders()[HttpHeaderNames.CONTENT_TYPE] = "text/plain; charset=UTF-8"
                    exchange.endExchange()
                } finally {
                    request.release()
                }
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
