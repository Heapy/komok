package io.heapy.komok.server.core

fun interface HttpHandler {
    suspend fun handle(exchange: HttpExchange)
}
