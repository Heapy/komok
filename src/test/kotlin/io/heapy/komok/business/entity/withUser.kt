package io.heapy.komok.business.entity

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class WithUserTest {
    object JwtConfig {
        private const val secret = "secret"
        private const val issuer = "ktor.io"
        private const val validityInMs = 36_000_00 * 10 // 10 hours

        val verifier = JWT
            .require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .build()

        fun makeToken(
            id: String,
            username: String,
            expiration: Date
        ): String {
            return JWT
                .create()
                .withClaim(
                    "id",
                    id
                )
                .withClaim(
                    "username",
                    username
                )
                .withIssuer(issuer)
                .withExpiresAt(expiration)
                .sign(Algorithm.HMAC256(secret))
        }
    }

    fun Application.testModule() {
        install(Authentication) {
            jwt {
                verifier(JwtConfig.verifier)
                validate { credential ->
                    if (credential.payload
                            .getClaim("id")
                            .asString() != ""
                    ) JWTPrincipal(credential.payload) else null
                }
            }
        }

        routing {
            authenticate {
                get("/protected") {
                    withUser {
                        call.respondText("Hello, ${user.id}")
                    }
                }
            }
        }
    }

    @Test
    fun `should correctly parse valid JWT and execute function`() =
        testApplication {
            application {
                testModule()
            }
            val token = JwtConfig.makeToken(
                "user123",
                "testuser",
                Date(System.currentTimeMillis() + 10000)
            )

            val response = client.get("/protected") {
                headers {
                    append(
                        HttpHeaders.Authorization,
                        "Bearer $token"
                    )
                }
            }
            assertEquals(
                HttpStatusCode.OK,
                response.status
            )
            assertEquals(
                "Hello, testuser! ID: user123 Expires in: 10000ms",
                response.bodyAsText()
            )
        }
}
