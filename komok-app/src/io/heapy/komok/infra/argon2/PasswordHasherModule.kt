package io.heapy.komok.infra.argon2

import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.random.RandomGenerator

@Module
class PasswordHasherModule {
    context(_: KotomiContext)
    suspend fun passwordHasher(): PasswordHasher =
        Argon2idPasswordHasher(
            json = json(),
            randomGenerator = randomGenerator(),
        )

    context(_: KotomiContext)
    suspend fun randomGenerator(): RandomGenerator =
        SecureRandom()

    context(_: KotomiContext)
    suspend fun json(): Json =
        Json
}
