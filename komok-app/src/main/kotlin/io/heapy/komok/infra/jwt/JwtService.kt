package io.heapy.komok.infra.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.tech.time.TimeSource
import java.time.temporal.ChronoUnit

interface JwtService {
    fun createToken(
        userContext: UserContext,
    ): String
}

internal class DefaultJwtService(
    private val jwtConfiguration: JwtConfiguration,
    private val timeSource: TimeSource,
) : JwtService {
    override fun createToken(
        userContext: UserContext,
    ): String {
        return JWT
            .create()
            .withAudience(jwtConfiguration.audience)
            .withIssuer(jwtConfiguration.issuer)
            .withClaim(
                "id",
                userContext.id,
            )
            .withExpiresAt(
                timeSource
                    .instant()
                    .plus(
                        jwtConfiguration.expiration.inWholeSeconds,
                        ChronoUnit.SECONDS,
                    ),
            )
            .sign(Algorithm.HMAC512(jwtConfiguration.secret))
    }
}

