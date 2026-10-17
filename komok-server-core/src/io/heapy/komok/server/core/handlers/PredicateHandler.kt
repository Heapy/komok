package io.heapy.komok.server.core.handlers

import io.heapy.komok.server.core.HttpHandler
import io.heapy.komok.server.core.NettyHttpServerExchange
import io.heapy.komok.server.core.predicate.Predicate

class PredicateHandler(
    private val predicate: Predicate,
    private val trueHandler: HttpHandler,
    private val falseHandler: HttpHandler,
) : HttpHandler {
    override suspend fun handleRequest(
        exchange: NettyHttpServerExchange,
    ) {
        val next = if (predicate.resolve(exchange)) trueHandler else falseHandler
        next.handleRequest(exchange)
    }
}

