package io.heapy.komok.infra.jwt

import io.heapy.komok.business.login.JwtConfiguration
import io.heapy.komok.infra.time.TimeSourceModule
import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module

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
