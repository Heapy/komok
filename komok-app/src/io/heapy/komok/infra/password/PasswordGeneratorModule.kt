package io.heapy.komok.infra.password

import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import java.security.SecureRandom

@Module
class PasswordGeneratorModule {
    context(_: KotomiContext)
    suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KotomiContext)
    suspend fun passwordGenerator(): PasswordGenerator =
        PasswordGenerator(
            random = random(),
        )
}
