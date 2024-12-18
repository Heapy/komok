package io.heapy.komok.server.core.predicate

import io.heapy.komok.server.core.NettyHttpServerExchange

interface Predicate {
    suspend fun resolve(
        exchange: NettyHttpServerExchange,
    ): Boolean
}
