package io.heapy.komok.infra.totp

import io.heapy.komok.infra.base32.Base32Module
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import io.heapy.kotomi.time.TimeSourceModule
import java.security.SecureRandom

@Module
class TimeBasedOneTimePasswordModule(
    private val base32Module: Base32Module,
    private val timeSourceModule: TimeSourceModule,
) {
    context(_: KotomiContext)
    suspend fun timeBasedOneTimePasswordService(): TimeBasedOneTimePasswordService =
        TimeBasedOneTimePasswordService(
            base32 = base32Module.base32(),
            timeSource = timeSourceModule.timeSource(),
        )

    context(_: KotomiContext)
    suspend fun random(): SecureRandom =
        SecureRandom()

    context(_: KotomiContext)
    suspend fun generateTimeBasedOneTimeKeyService(): GenerateTimeBasedOneTimeKeyService =
        GenerateTimeBasedOneTimeKeyService(
            base32 = base32Module.base32(),
            random = random(),
        )
}
