package io.heapy.komok.infra.argon2

import io.heapy.komok.tech.di.lib.Module
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.random.RandomGenerator

@Module
open class PasswordHasherModule {
    open val passwordHasher: PasswordHasher by lazy {
        Argon2idPasswordHasher(
            json = json,
            randomGenerator = randomGenerator,
        )
    }

    open val randomGenerator: RandomGenerator by lazy {
        SecureRandom()
    }

    open val json: Json by lazy {
        Json
    }
}
