//package io.heapy.vipassana.http
//
//import io.netty.bootstrap.ServerBootstrap
//import io.netty.buffer.ByteBufInputStream
//import io.netty.channel.ChannelFactory
//import io.netty.channel.ChannelFuture
//import io.netty.channel.ChannelHandlerContext
//import io.netty.channel.ChannelInboundHandlerAdapter
//import io.netty.channel.ChannelInitializer
//import io.netty.channel.ChannelOption
//import io.netty.channel.ServerChannel
//import io.netty.channel.SimpleChannelInboundHandler
//import io.netty.channel.nio.NioEventLoopGroup
//import io.netty.channel.socket.SocketChannel
//import io.netty.channel.socket.nio.NioServerSocketChannel
//import io.netty.handler.codec.http.DefaultHttpResponse
//import io.netty.handler.codec.http.FullHttpRequest
//import io.netty.handler.codec.http.HttpHeaderNames
//import io.netty.handler.codec.http.HttpHeaderValues
//import io.netty.handler.codec.http.HttpObjectAggregator
//import io.netty.handler.codec.http.HttpRequest
//import io.netty.handler.codec.http.HttpResponseStatus
//import io.netty.handler.codec.http.HttpServerCodec
//import io.netty.handler.codec.http.HttpServerKeepAliveHandler
//import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
//import io.netty.handler.codec.http.LastHttpContent
//import io.netty.handler.stream.ChunkedStream
//import io.netty.handler.stream.ChunkedWriteHandler
//import java.net.InetSocketAddress
//import io.netty.handler.codec.http.DefaultFullHttpResponse
//import io.netty.handler.codec.http.HttpHeaderNames.CONNECTION
//import io.netty.handler.codec.http.HttpHeaderValues.UPGRADE
//import io.netty.handler.codec.http.HttpHeaderValues.WEBSOCKET
//import io.netty.handler.codec.http.HttpVersion
//import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
//import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
//import io.netty.buffer.Unpooled
//import io.netty.buffer.Unpooled.EMPTY_BUFFER
//import io.netty.channel.ChannelFutureListener
//import io.netty.channel.ChannelFutureListener.CLOSE
//import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
//import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
//import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
//import io.netty.handler.codec.http.websocketx.WebSocketFrame
//import java.io.InputStream
//
///**
// * Exposed to allow for insertion into a customised Netty server instance
// */
//internal class SVChannelHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
//    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
//        val address = ctx.channel().remoteAddress() as InetSocketAddress
//
//        request.apply {
//            println(method().name())
//            println(uri())
//            println(headers())
//            println(ByteBufInputStream(content()))
//            println(headers()["Content-Length"].toLongOrNull())
//            println(address.address.hostAddress)
//            println(address.port)
//        }
//
//        val response = DefaultHttpResponse(
//            HTTP_1_1,
//            HttpResponseStatus(200, "OK")
//        ).apply {
//            mapOf(
//                "hello" to "world"
//            ).forEach { (key, values) -> headers().set(key, values) }
//        }
//
//        val stream = ChunkedStream("body.stream".byteInputStream())
//
//        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
//
//        ctx.write(response)
//        ctx.write(stream)
//        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
//    }
//}
//
//fun main() {
//    Netty().start()
//
//}
//
//internal class Netty(
//    val port: Int = 8000
//) {
//    // TODO: Automatically select native impl (see ktor for example)
//    private val masterGroup = NioEventLoopGroup(1)
//    private val workerGroup = NioEventLoopGroup()
//    private var closeFuture: ChannelFuture? = null
//    private lateinit var address: InetSocketAddress
//
//    fun start() {
//        val bootstrap = ServerBootstrap()
//        bootstrap.group(masterGroup, workerGroup)
//            .channelFactory(ChannelFactory<ServerChannel> { NioServerSocketChannel() })
//            .childHandler(object : ChannelInitializer<SocketChannel>() {
//                public override fun initChannel(ch: SocketChannel) {
//                    ch.pipeline().addLast("codec", HttpServerCodec())
//                    ch.pipeline().addLast("keepAlive", HttpServerKeepAliveHandler())
//                    ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))
////                    ch.pipeline().addLast("websocket", WebSocketServerHandler())
//                    ch.pipeline().addLast("streamer", ChunkedWriteHandler())
//                    ch.pipeline().addLast("httpHandler", SVChannelHandler())
//                }
//            })
//            .option(ChannelOption.SO_BACKLOG, 1000)
//            .childOption(ChannelOption.SO_KEEPALIVE, true)
//
//        val channel = bootstrap.bind(port).sync().channel()
//        address = channel.localAddress() as InetSocketAddress
//        closeFuture = channel.closeFuture()
//    }
//
//    fun stop() = apply {
//        closeFuture?.cancel(false)
//        workerGroup.shutdownGracefully()
//        masterGroup.shutdownGracefully()
//    }
//
//    fun port(): Int = if (port > 0) port else address.port
//}
////
////internal class WebSocketServerHandler : ChannelInboundHandlerAdapter() {
////    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
////        if (msg is HttpRequest) {
////            if (requiresWsUpgrade(msg)) {
////                val address = ctx.channel().remoteAddress() as InetSocketAddress
////                val upgradeRequest = msg.asRequest(address)
////                val wsConsumer = wsHandler(upgradeRequest)
////
////                val config = WebSocketServerProtocolConfig.newBuilder()
////                    .handleCloseFrames(false)
////                    .websocketPath(upgradeRequest.uri.toString())
////                    .checkStartsWith(true)
////                    .build()
////
////                ctx.pipeline().addAfter(
////                    ctx.name(),
////                    "handshakeListener",
////                    object : ChannelInboundHandlerAdapter() {
////                        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
////                            if (evt is WebSocketServerProtocolHandler.HandshakeComplete) {
////                                ctx.pipeline().addAfter(
////                                    ctx.name(),
////                                    Http4kWsChannelHandler::class.java.name,
////                                    Http4kWsChannelHandler(wsConsumer, upgradeRequest)
////                                )
////                            }
////                        }
////                    }
////                )
////
////                ctx.pipeline().addAfter(
////                    ctx.name(),
////                    WebSocketServerProtocolHandler::class.java.name,
////                    WebSocketServerProtocolHandler(config)
////                )
////
////                ctx.fireChannelRead(msg)
////            } else {
////                ctx.fireChannelRead(msg)
////            }
////        } else {
////            ctx.fireChannelRead(msg)
////        }
////    }
////
////    private fun requiresWsUpgrade(httpRequest: HttpRequest) =
////        httpRequest.headers().containsValue(CONNECTION, UPGRADE, true) &&
////            httpRequest.headers().containsValue(UPGRADE, WEBSOCKET, true)
////
////    private fun HttpRequest.asRequest(address: InetSocketAddress) =
////        Request(Method.valueOf(method().name()), Uri.of(uri()))
////            .headers(headers().map { it.key to it.value })
////            .source(RequestSource(address.address.hostAddress, address.port))
////}
////
////internal class Http4kWsChannelHandler(
////    private val wSocket: WsConsumer,
////    private val upgradeRequest: Request
////) : SimpleChannelInboundHandler<WebSocketFrame>() {
////    private var websocket: PushPullAdaptingWebSocket? = null
////    private var normalClose = false
////
////    override fun handlerAdded(ctx: ChannelHandlerContext) {
////        websocket = object : PushPullAdaptingWebSocket(upgradeRequest) {
////            override fun send(message: WsMessage) {
////                when (message.body) {
////                    is StreamBody -> ctx.writeAndFlush(BinaryWebSocketFrame(message.body.stream.use { Unpooled.wrappedBuffer(it.readBytes()) }))
////                    else -> ctx.writeAndFlush(TextWebSocketFrame(message.bodyString()))
////                }
////            }
////
////            override fun close(status: WsStatus) {
////                ctx.writeAndFlush(CloseWebSocketFrame(status.code, status.description))
////                    .addListeners(ChannelFutureListener {
////                        normalClose = true
////                        websocket?.triggerClose(status)
////                    }, CLOSE)
////            }
////        }.apply(wSocket)
////    }
////
////    override fun handlerRemoved(ctx: ChannelHandlerContext) {
////        if (!normalClose) {
////            ctx.writeAndFlush(EMPTY_BUFFER).addListeners(ChannelFutureListener {
////                websocket?.triggerClose(NOCODE)
////            }, CLOSE)
////        }
////        websocket = null
////    }
////
////    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
////        when (msg) {
////            is TextWebSocketFrame -> websocket?.triggerMessage(WsMessage(Body(msg.text())))
////            is BinaryWebSocketFrame -> websocket?.triggerMessage(WsMessage(Body(ByteBufInputStream(msg.content()))))
////            is CloseWebSocketFrame -> {
////                msg.retain()
////                ctx.writeAndFlush(msg).addListeners(ChannelFutureListener {
////                    normalClose = true
////                    websocket?.triggerClose(WsStatus(msg.statusCode(), msg.reasonText()))
////                }, CLOSE)
////            }
////        }
////    }
////
////    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
////        websocket?.triggerError(cause)
////    }
////}
////
////internal abstract class PushPullAdaptingWebSocket(override val upgradeRequest: Request) : Websocket {
////
////    private val errorHandlers: MutableList<(Throwable) -> Unit> = mutableListOf()
////    private val closeHandlers: MutableList<(WsStatus) -> Unit> = mutableListOf()
////    private val messageHandlers: MutableList<(WsMessage) -> Unit> = mutableListOf()
////
////    fun triggerError(throwable: Throwable) = errorHandlers.forEach { it(throwable) }
////    fun triggerClose(status: WsStatus = NORMAL) = closeHandlers.forEach { it(status) }
////    fun triggerMessage(message: WsMessage) = messageHandlers.forEach { it(message) }
////
////    override fun onError(fn: (Throwable) -> Unit) {
////        errorHandlers.add(fn)
////    }
////
////    override fun onClose(fn: (WsStatus) -> Unit) {
////        closeHandlers.add(fn)
////    }
////
////    override fun onMessage(fn: (WsMessage) -> Unit) {
////        messageHandlers.add(fn)
////    }
////}
////
/////**
//// * Represents a connected Websocket instance, and can be passed around an application. This is configured
//// * to react to events on the WS event stream by attaching listeners.
//// */
////internal interface Websocket {
////    val upgradeRequest: Request
////    fun send(message: WsMessage)
////    fun close(status: WsStatus = NORMAL)
////    fun onError(fn: (Throwable) -> Unit)
////    fun onClose(fn: (WsStatus) -> Unit)
////    fun onMessage(fn: (WsMessage) -> Unit)
////}
////
////internal typealias WsConsumer = (Websocket) -> Unit
////
////internal typealias WsHandler = (Request) -> WsConsumer
////
////internal data class WsMessage(val body: Body) {
////    constructor(value: String) : this(Body(value))
////    constructor(value: InputStream) : this(Body(value))
////
////    fun body(new: Body): WsMessage = copy(body = new)
////    fun bodyString(): String = String(body.payload.array())
////
////    companion object
////}
////
////internal fun interface WsFilter : (WsConsumer) -> WsConsumer {
////    companion object
////}
////
////internal data class WsStatus(val code: Int, val description: String) {
////    companion object {
////        val NORMAL = WsStatus(1000, "Normal")
////        val GOING_AWAY = WsStatus(1001, "Going away")
////        val PROTOCOL_ERROR = WsStatus(1002, "Protocol error")
////        val REFUSE = WsStatus(1003, "Refuse")
////        val NOCODE = WsStatus(1005, "No code")
////        val ABNORMAL_CLOSE = WsStatus(1006, "Abnormal close")
////        val NO_UTF8 = WsStatus(1007, "No UTF8")
////        val POLICY_VALIDATION = WsStatus(1008, "Policy validation")
////        val TOOBIG = WsStatus(1009, "Too big")
////        val EXTENSION = WsStatus(1010, "Extension")
////        val UNEXPECTED_CONDITION = WsStatus(1011, "Unexpected condition")
////        val TLS_ERROR = WsStatus(1015, "TLS error")
////        val NEVER_CONNECTED = WsStatus(-1, "Never connected")
////        val BUGGYCLOSE = WsStatus(-2, "Buggy close")
////        val FLASHPOLICY = WsStatus(-3, "Flash policy")
////    }
////
////    fun description(description: String) = copy(description = description)
////
////    override fun equals(other: Any?): Boolean = other != null && other is WsStatus && other.code == code
////    override fun hashCode(): Int = code.hashCode()
////    override fun toString(): String = "$code $description"
////}
