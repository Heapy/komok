package io.heapy.komok.infra.session_token

import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import java.security.SecureRandom

@Module
class SessionTokenServiceModule {
    context(_: KotomiContext)
    suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KotomiContext)
    suspend fun sessionTokenService(): SessionTokenService =
        SecureRandomSessionTokenService(
            random = random(),
        )
}
