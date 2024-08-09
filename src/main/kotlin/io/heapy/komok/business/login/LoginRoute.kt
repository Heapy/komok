package io.heapy.komok.business.login

import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

class LoginRoute(
    private val jwtService: JwtService,
) : KomokRoute {
    @Serializable
    data class LoginRequest(
        val email: String,
        val password: String,
    )

    override fun Routing.install() {
        post("/login") {
            val req = call.receive<LoginRequest>()
//            jwtService.createToken(User(id = "1"))
//            call.respond(hashMapOf("token" to token))
        }
    }
}
