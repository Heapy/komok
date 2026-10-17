package io.heapy.komok.business.user.me

import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.plugins.origin
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MeRoute(
    private val meService: MeService,
) : KomokRoute {
    override fun Routing.install() {
        get("/me") {
            val forwardedFor = call.request.headers["X-Forwarded-For"]
            val clientIp = forwardedFor
                ?: call.request.origin.remoteHost
            val session = call.request.cookies["JSESSIONID"]
                ?: error("Session not found")

            call.respond(
                meService
                    .getMe(
                        ip = clientIp,
                        session = session,
                    )
            )
        }
    }
}
