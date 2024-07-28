package io.heapy.komok.infra.server

import io.ktor.server.routing.Route

interface KomokRoute {
    fun Route.install()
}
