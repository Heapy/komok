package io.heapy.komok.infra.session_token

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import java.security.SecureRandom

@Module
class SessionTokenServiceModule {
    context(_: KomokContext)
    suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KomokContext)
    suspend fun sessionTokenService(): SessionTokenService =
        SecureRandomSessionTokenService(
            random = random(),
        )
}
