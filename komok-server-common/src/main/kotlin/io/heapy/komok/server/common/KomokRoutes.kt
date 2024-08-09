package io.heapy.komok.server.common

import io.ktor.server.routing.*

class KomokRoutes(
    private val routes: List<KomokRoute>,
) : KomokRoute {
    override fun Routing.install() {
        routes.forEach { route ->
            route.run { install() }
        }
    }
}
