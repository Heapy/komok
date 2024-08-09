package io.heapy.komok.server.core

fun interface HttpHandlerWrapper {
    suspend fun wrap(handler: HttpHandler): HttpHandler
}
