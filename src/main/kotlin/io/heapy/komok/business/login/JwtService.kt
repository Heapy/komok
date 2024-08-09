package io.heapy.komok.business.login

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.heapy.komok.TimeSourceContext
import io.heapy.komok.auth.common.User
import io.heapy.komok.configuration.ConfigModule
import io.heapy.komok.tech.di.lib.Module
import java.time.temporal.ChronoUnit

interface JwtService {
    context(TimeSourceContext)
    fun createToken(
        user: User,
    ): String
}

private class DefaultJwtService(
    private val jwtConfiguration: JwtConfiguration,
) : JwtService {
    context(TimeSourceContext)
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
    private val configModule: ConfigModule,
) {
    open val config: JwtConfiguration by lazy {
        configModule.config.read(
            deserializer = JwtConfiguration.serializer(),
            path = "jwt",
        ).also { config ->
            require(config.secret.length >= 128) {
                "Secret must be at least 128 characters long"
            }
        }
    }

    open val jwtService: JwtService by lazy {
        DefaultJwtService(
            jwtConfiguration = config,
        )
    }
}
