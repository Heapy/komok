package io.heapy.komok.server.common

import io.ktor.server.routing.Routing

interface KomokRoute {
    fun Routing.install()
}
