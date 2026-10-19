package io.heapy.komok.infra.totp

import io.heapy.komok.infra.base32.Base32Module
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.time.TimeSourceModule
import java.security.SecureRandom

@Module
class TimeBasedOneTimePasswordModule(
    private val base32Module: Base32Module,
    private val timeSourceModule: TimeSourceModule,
) {
    context(_: KomokContext)
    suspend fun timeBasedOneTimePasswordService(): TimeBasedOneTimePasswordService =
        TimeBasedOneTimePasswordService(
            base32 = base32Module.base32(),
            timeSource = timeSourceModule.timeSource(),
        )

    context(_: KomokContext)
    suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KomokContext)
    suspend fun generateTimeBasedOneTimeKeyService(): GenerateTimeBasedOneTimeKeyService =
        GenerateTimeBasedOneTimeKeyService(
            base32 = base32Module.base32(),
            random = random(),
        )
}
