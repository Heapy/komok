package io.heapy.komok.infra.http.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.heapy.komok.infra.http.server.errors.AuthenticationException
import io.heapy.komok.infra.http.server.errors.AuthorizationException
import io.heapy.komok.infra.http.server.errors.BadRequestException
import io.heapy.komok.infra.jwt.JwtConfiguration
import io.heapy.komok.server.common.KomokServerFeature
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import kotlin.time.Duration.Companion.days

class DefaultFeature(
    private val jwtConfiguration: JwtConfiguration,
) : KomokServerFeature {
    override fun Application.install() {
        defaults(jwtConfiguration)
    }
}

fun Application.defaults(
    jwtConfiguration: JwtConfiguration,
) {
    install(Resources)

    install(ContentNegotiation) {
        json()
    }

    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS -> CachingOptions(
                    CacheControl.MaxAge(
                        maxAgeSeconds = 365.days
                            .inWholeSeconds
                            .toInt()
                    )
                )

                else -> null
            }
        }
    }

    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, cause.response)
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, cause.response)
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.response)
        }
    }

    authentication {
        jwt("jwt") {
            realm = jwtConfiguration.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC512(jwtConfiguration.secret))
                    .withAudience(jwtConfiguration.audience)
                    .withIssuer(jwtConfiguration.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtConfiguration.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
