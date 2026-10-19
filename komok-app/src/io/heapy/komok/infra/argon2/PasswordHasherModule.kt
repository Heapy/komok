package io.heapy.komok.infra.argon2

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.random.RandomGenerator

@Module
class PasswordHasherModule {
    context(_: KomokContext)
    suspend fun passwordHasher(): PasswordHasher =
        Argon2idPasswordHasher(
            json = json(),
            randomGenerator = randomGenerator(),
        )

    context(_: KomokContext)
    suspend fun randomGenerator(): RandomGenerator =
        SecureRandom()

    context(_: KomokContext)
    suspend fun json(): Json =
        Json
}
