@file:OptIn(KtorExperimentalLocationsAPI::class)

package io.heapy.komok.server.routes

import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Location("/ping/{name}")
class PingRequest(val name: String)

fun Routing.ping() {
    get<PingRequest> {
        call.respondText("Pong: ${it.name}")
    }
}
