package io.heapy.komok.business.login

import io.heapy.komok.server.common.KomokRoute
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

class LoginRoute(
    private val loginService: LoginService,
) : KomokRoute {
    override fun Routing.install() {
        post("/login") {
            val loginRequest = call.receive<LoginRequest>()
            val forwardedFor = call.request.headers["X-Forwarded-For"]
            val clientIp = forwardedFor ?: call.request.origin.remoteHost

            val loginResponse = loginService
                .login(
                    loginRequest = loginRequest,
                    ip = clientIp,
                    userAgent = call.request.headers["User-Agent"] ?: "Unknown",
                )

            call.response.cookies.append(
                Cookie(
                    name = "JSESSIONID",
                    value = loginResponse.sessionToken,
                    maxAge = loginResponse.maxAge.inWholeSeconds.toInt(),
                    path = "/",
                    secure = true,
                    httpOnly = true,
                )
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
