package io.heapy.komok.infra.http.server

import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.auth.*
import io.ktor.server.routing.*

class AuthenticatedRoutes(
    private val routes: KomokRoute,
) : KomokRoute {
    override fun Routing.install() {
        authenticate("jwt") {
            route("/api") {
                routes.run { install() }
            }
        }
    }
}
