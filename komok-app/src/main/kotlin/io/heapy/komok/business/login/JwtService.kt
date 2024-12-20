package io.heapy.komok.business.login

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.heapy.komok.infra.time.TimeSource
import io.heapy.komok.auth.common.User
import io.heapy.komok.infra.time.TimeSourceModule
import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module
import java.time.temporal.ChronoUnit

interface JwtService {
    fun createToken(
        user: User,
    ): String
}

private class DefaultJwtService(
    private val jwtConfiguration: JwtConfiguration,
    private val timeSource: TimeSource,
) : JwtService {
    override fun createToken(
        user: User,
    ): String {
        return JWT
            .create()
            .withAudience(jwtConfiguration.audience)
            .withIssuer(jwtConfiguration.issuer)
            .withClaim(
                "id",
                user.id,
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

@Module
open class JwtModule(
    private val configurationModule: ConfigurationModule,
    private val timeSourceModule: TimeSourceModule,
) {
    open val jwtConfiguration: JwtConfiguration by lazy {
        configurationModule
            .config
            .read(
                deserializer = JwtConfiguration.serializer(),
                path = "jwt",
            )
            .also { config ->
                require(config.secret.length >= 128) {
                    "Secret must be at least 128 characters long"
                }
            }
    }

    open val jwtService: JwtService by lazy {
        DefaultJwtService(
            jwtConfiguration = jwtConfiguration,
            timeSource = timeSourceModule.timeSource,
        )
    }
}
