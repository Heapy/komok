package io.heapy.komok.business.healthcheck

import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

class HealthCheckRoute : KomokRoute {
    @Serializable
    data class HealthCheckResponse(val status: String)

    private val status = HealthCheckResponse("OK")

    override fun Route.install() {
        get("/api/healthcheck") {
            call.respond(status)
        }
    }
}
