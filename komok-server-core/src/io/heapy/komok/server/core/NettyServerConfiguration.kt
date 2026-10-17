package io.heapy.komok.server.core

import kotlinx.serialization.Serializable

interface NettyServerConfiguration {
    val port: Int
    val shutdownTimeout: Int
    val keepAlive: Boolean
    val backlog: Int
}

@Serializable
data class DefaultNettyServerConfiguration(
    override val port: Int,
    override val shutdownTimeout: Int,
    override val keepAlive: Boolean,
    override val backlog: Int,
) : NettyServerConfiguration
