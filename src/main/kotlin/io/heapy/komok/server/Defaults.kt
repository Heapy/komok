@file:OptIn(KtorExperimentalLocationsAPI::class)

package io.heapy.komok.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days


fun Application.defaults(
    jwtConfiguration: JwtConfiguration,
) {
    install(Locations)

    install(ContentNegotiation) {
        json()
    }

    install(DefaultHeaders) {
        header("X-Application", "komok")
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
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<ConstraintViolationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.fields)
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

@Serializable
data class JwtConfiguration(
    val audience: String,
    val realm: String,
    val issuer: String,
    val secret: String,
)

class AuthenticationException : RuntimeException()

class AuthorizationException : RuntimeException()

class ConstraintViolationException(
    val fields: List<ConstraintViolationFields>,
) : RuntimeException()

@Serializable
data class ConstraintViolationFields(
    val message: String,
    val fields: List<String>,
)
