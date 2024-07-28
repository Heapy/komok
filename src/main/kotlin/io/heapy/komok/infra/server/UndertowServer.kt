package io.heapy.komok.infra.server

import io.heapy.komok.business.ServerConfiguration
import io.undertow.Undertow
import io.undertow.server.HttpHandler

class UndertowServer(
    private val serverConfiguration: ServerConfiguration,
    private val rootHandler: HttpHandler,
) {
    fun start(): Undertow {
        val instance = Undertow
            .builder()
            .setHandler(rootHandler)
            .addHttpListener(
                serverConfiguration.port,
                serverConfiguration.host,
            )
            .build()

        instance.start()

        return instance
    }
}

fun rootHandler(): HttpHandler {
    return HttpHandler { exchange ->
        exchange.responseSender.send("Hello, world!")
    }
}
