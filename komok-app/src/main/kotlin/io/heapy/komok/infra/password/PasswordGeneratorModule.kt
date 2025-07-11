package io.heapy.komok.infra.password

import io.heapy.komok.tech.di.lib.Module
import java.security.SecureRandom

@Module
open class PasswordGeneratorModule {
    open val random by lazy {
        SecureRandom()
    }

    open val passwordGenerator by lazy {
        PasswordGenerator(
            random = random,
        )
    }
}
