package io.heapy.komok.server.core.handlers

import io.heapy.komok.server.core.ExchangeCompletionListener
import io.heapy.komok.server.core.HttpHandler
import io.heapy.komok.server.core.NettyHttpServerExchange

class AccessLogHandler(
    private val next: HttpHandler,
    private val logHandler: HttpHandler,
) : HttpHandler {
    override suspend fun handleRequest(
        exchange: NettyHttpServerExchange,
    ) {
        exchange.addExchangeCompleteListener(object : ExchangeCompletionListener {
            override fun exchangeEvent(
                exchange: NettyHttpServerExchange,
                nextListener: ExchangeCompletionListener.NextListener,
            ) {
            }
        })
        next.handleRequest(exchange)
    }
}
