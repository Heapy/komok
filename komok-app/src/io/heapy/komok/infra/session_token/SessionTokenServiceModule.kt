package io.heapy.komok.infra.session_token

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import java.security.SecureRandom

@Module
open class SessionTokenServiceModule {
    context(_: KomokContext)
    open suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KomokContext)
    open suspend fun sessionTokenService(): SessionTokenService =
        SecureRandomSessionTokenService(
            random = random(),
        )
}
