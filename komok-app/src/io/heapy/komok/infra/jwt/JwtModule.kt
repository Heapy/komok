package io.heapy.komok.infra.jwt

import io.heapy.kotomi.config.ConfigurationModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import io.heapy.kotomi.time.TimeSourceModule

@Module
class JwtModule(
    private val configurationModule: ConfigurationModule,
    private val timeSourceModule: TimeSourceModule,
) {
    context(_: KotomiContext)
    suspend fun jwtConfiguration(): JwtConfiguration =
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

    context(_: KotomiContext)
    suspend fun jwtService(): JwtService =
        DefaultJwtService(
            jwtConfiguration = jwtConfiguration(),
            timeSource = timeSourceModule.timeSource(),
        )
}
