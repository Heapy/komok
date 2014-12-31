//package io.heapy.vipassana.http
//
//import io.netty.bootstrap.ServerBootstrap
//import io.netty.channel.Channel
//import io.netty.channel.ChannelInitializer
//import io.netty.channel.ChannelPipeline
//import io.netty.channel.EventLoopGroup
//import io.netty.channel.nio.NioEventLoopGroup
//import io.netty.channel.socket.SocketChannel
//import io.netty.channel.socket.nio.NioServerSocketChannel
//import io.netty.handler.codec.http.HttpObjectAggregator
//import io.netty.handler.codec.http.HttpServerCodec
//import io.netty.handler.logging.LogLevel
//import io.netty.handler.logging.LoggingHandler
//import io.netty.handler.stream.ChunkedWriteHandler
//import io.netty5.bootstrap.ServerBootstrap
//import io.netty5.channel.EventLoopGroup
//import io.netty5.handler.ssl.SslContext
//import io.netty5.handler.ssl.SslContextBuilder
//import io.netty5.handler.ssl.SslProvider
//import io.netty5.handler.ssl.util.SelfSignedCertificate
//
//public suspend fun main() {
//
//}
//
//internal object HttpStaticFileServer {
//    val SSL = System.getProperty("ssl") != null
//    val PORT = System.getProperty("port", if (SSL) "8443" else "8080").toInt()
//
//    @Throws(Exception::class)
//    @JvmStatic
//    fun main(args: Array<String>) {
//        // Configure SSL.
//        val sslCtx: SslContext? = if (SSL) {
//            val ssc = SelfSignedCertificate()
//            SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
//                .sslProvider(SslProvider.JDK).build()
//        } else {
//            null
//        }
//        val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
//        val workerGroup: EventLoopGroup = NioEventLoopGroup()
//        try {
//            val b = ServerBootstrap()
//            b.group(bossGroup, workerGroup)
//                .channel(NioServerSocketChannel::class.java)
//                .handler(LoggingHandler(LogLevel.INFO))
//                .childHandler(HttpStaticFileServerInitializer(sslCtx))
//            val ch: Channel = b.bind(PORT).sync().channel()
//            System.err.println("Open your web browser and navigate to " + (if (SSL) "https" else "http") + "://127.0.0.1:" + PORT + '/')
//            ch.closeFuture().sync()
//        } finally {
//            bossGroup.shutdownGracefully()
//            workerGroup.shutdownGracefully()
//        }
//    }
//}
//
//internal class HttpStaticFileServerInitializer(private val sslCtx: SslContext?) : ChannelInitializer<SocketChannel>() {
//    public override fun initChannel(ch: SocketChannel) {
//        val pipeline: ChannelPipeline = ch.pipeline()
//        if (sslCtx != null) {
//            pipeline.addLast(sslCtx.newHandler(ch.alloc()))
//        }
//        pipeline.addLast(HttpServerCodec())
//        pipeline.addLast(HttpObjectAggregator(65536))
//        pipeline.addLast(ChunkedWriteHandler())
//        pipeline.addLast(HttpStaticFileServerHandler())
//    }
//}
