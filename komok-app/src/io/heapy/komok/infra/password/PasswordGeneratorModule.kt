package io.heapy.komok.infra.password

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import java.security.SecureRandom

@Module
class PasswordGeneratorModule {
    context(_: KomokContext)
    suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KomokContext)
    suspend fun passwordGenerator(): PasswordGenerator =
        PasswordGenerator(
            random = random(),
        )
}
