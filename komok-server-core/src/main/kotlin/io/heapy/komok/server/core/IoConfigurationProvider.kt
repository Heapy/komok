package io.heapy.komok.server.core

import io.netty.channel.IoHandlerFactory
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.uring.IoUring
import io.netty.channel.uring.IoUringIoHandler
import io.netty.channel.uring.IoUringServerSocketChannel

interface IoConfigurationProvider {
    operator fun invoke(): IoConfiguration
}

class SystemDependentIoConfigurationProvider : IoConfigurationProvider {
    override fun invoke(): IoConfiguration =
        when {
            // Prioritize io_uring over epoll
            IoUring.isAvailable() -> DefaultIoConfiguration(
                ioHandlerFactory = IoUringIoHandler.newFactory(),
                serverSocketChannel = IoUringServerSocketChannel::class.java,
            )
            KQueue.isAvailable() -> DefaultIoConfiguration(
                ioHandlerFactory = KQueueIoHandler.newFactory(),
                serverSocketChannel = KQueueServerSocketChannel::class.java,
            )
            Epoll.isAvailable() -> DefaultIoConfiguration(
                ioHandlerFactory = EpollIoHandler.newFactory(),
                serverSocketChannel = EpollServerSocketChannel::class.java,
            )
            // Default to NIO
            else -> DefaultIoConfiguration(
                ioHandlerFactory = NioIoHandler.newFactory(),
                serverSocketChannel = NioServerSocketChannel::class.java,
            )
        }
}

interface IoConfiguration {
    val ioHandlerFactory: IoHandlerFactory
    val serverSocketChannel: Class<out ServerSocketChannel>
}

private data class DefaultIoConfiguration(
    override val ioHandlerFactory: IoHandlerFactory,
    override val serverSocketChannel: Class<out ServerSocketChannel>,
) : IoConfiguration
