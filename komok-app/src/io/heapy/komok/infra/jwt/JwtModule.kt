package io.heapy.komok.infra.jwt

import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.time.TimeSourceModule

@Module
open class JwtModule(
    private val configurationModule: ConfigurationModule,
    private val timeSourceModule: TimeSourceModule,
) {
    context(_: KomokContext)
    open suspend fun jwtConfiguration(): JwtConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = JwtConfiguration.serializer(),
                path = "jwt",
            )
            .also { config ->
                require(config.secret.length >= 128) {
                    "Secret must be at least 128 characters long"
                }
            }

    context(_: KomokContext)
    open suspend fun jwtService(): JwtService =
        DefaultJwtService(
            jwtConfiguration = jwtConfiguration(),
            timeSource = timeSourceModule.timeSource(),
        )
}
