package io.heapy.komok.business.login

import io.heapy.komok.infra.server.KomokRoute
import io.heapy.komok.wip.TransactionContextProvider
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

class LoginRoute(
    private val transactionContextProvider: TransactionContextProvider,
    private val jwtService: JwtService,
) : KomokRoute {
    data class LoginRequest(
        val email: String,
        val password: String,
    )

    override fun Route.install() {
        post("/login") {
            val req = call.receive<LoginRequest>()
//            jwtService.createToken(User(id = "1"))
//            call.respond(hashMapOf("token" to token))
        }
    }
}
