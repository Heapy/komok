package io.heapy.komok.infra.password

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import java.security.SecureRandom

@Module
open class PasswordGeneratorModule {
    context(_: KomokContext)
    open suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KomokContext)
    open suspend fun passwordGenerator(): PasswordGenerator =
        PasswordGenerator(
            random = random(),
        )
}
