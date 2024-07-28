package io.heapy.komok.business.login

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.heapy.komok.TimeSourceContext
import io.heapy.komok.User
import io.heapy.komok.configuration.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
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
    private val configurationModule: ConfigurationModule,
) {
    @OptIn(ExperimentalSerializationApi::class)
    open val jwtConfiguration by lazy {
        Hocon
            .decodeFromConfig<JwtConfiguration>(configurationModule.config.getConfig("jwt"))
            .also { config ->
                require(config.secret.length >= 128) {
                    "Secret must be at least 128 characters long"
                }
            }
    }

    open val jwtService by lazy<JwtService> {
        DefaultJwtService(
            jwtConfiguration = jwtConfiguration,
        )
    }
}
