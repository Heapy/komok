package io.heapy.komok.business.healthcheck

import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

class HealthCheckRoute : KomokRoute {
    @Serializable
    data class HealthCheckResponse(val status: String)

    private val status = HealthCheckResponse("OK")

    override fun Routing.install() {
        get("/api/healthcheck") {
            call.respond(status)
        }
    }
}
