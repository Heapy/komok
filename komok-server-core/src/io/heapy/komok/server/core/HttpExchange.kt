package io.heapy.komok.server.core

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.QueryStringDecoder
import org.xnio.ChannelListener

class NettyHttpServerExchange(
    private val ctx: ChannelHandlerContext,
    val request: FullHttpRequest
) {
    private val responseHeaders = DefaultHttpHeaders()
    private var responseStatus: HttpResponseStatus = HttpResponseStatus.OK
    private var responseContent: Any? = null

    // Request methods
    fun getRequestMethod(): HttpMethod = request.method()
    fun getRequestUri(): String = request.uri()
    fun getRequestHeaders(): HttpHeaders = request.headers()
    fun getQueryParameters(): Map<String, List<String>> {
        val decoder = QueryStringDecoder(request.uri())
        return decoder.parameters()
    }
    fun getRequestBody(): String = request.content().toString(Charsets.UTF_8)

    // Response methods
    fun setStatusCode(code: Int) {
        responseStatus = HttpResponseStatus.valueOf(code)
    }

    fun getResponseHeaders(): HttpHeaders = responseHeaders
    fun setResponseContent(content: Any) {
        responseContent = content
    }

    // Send the response
    fun endExchange() {
        val keepAlive = HttpUtil.isKeepAlive(request)

        val response = DefaultFullHttpResponse(
            request.protocolVersion(),
            responseStatus
        )

        // Set response headers
        response.headers().add(responseHeaders)

        // Set response content
        if (responseContent != null) {
            val bytes = when (responseContent) {
                is String -> (responseContent as String).toByteArray(Charsets.UTF_8)
                is ByteArray -> responseContent as ByteArray
                else -> responseContent.toString().toByteArray(Charsets.UTF_8)
            }
            response.content().writeBytes(bytes)
            response.headers()[HttpHeaderNames.CONTENT_LENGTH] = bytes.size.toString()
        } else {
            response.headers()[HttpHeaderNames.CONTENT_LENGTH] = "0"
        }

        // Handle keep-alive
        if (keepAlive) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
        } else {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
        }

        val future = ctx.writeAndFlush(response)

        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE)
        }
    }

    fun addExchangeCompleteListener(
        listener: ExchangeCompletionListener,
    ) {
        ctx.channel()
            .closeFuture()
            .addListener { future ->
            }
    }
}

interface ExchangeCompletionListener {
    fun exchangeEvent(
        exchange: NettyHttpServerExchange,
        nextListener: NextListener,
    )

    interface NextListener {
        fun proceed()
    }
}
