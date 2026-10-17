package io.heapy.komok.infra.session_token

import io.heapy.komok.tech.di.lib.Module
import java.security.SecureRandom

@Module
open class SessionTokenServiceModule {
    open val random by lazy {
        SecureRandom()
    }

    open val sessionTokenService by lazy {
        SecureRandomSessionTokenService(
            random = random,
        )
    }
}
