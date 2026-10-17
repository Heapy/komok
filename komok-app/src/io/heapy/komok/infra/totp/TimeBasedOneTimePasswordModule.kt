package io.heapy.komok.infra.totp

import io.heapy.komok.infra.base32.Base32Module
import io.heapy.komok.tech.di.lib.Module
import io.heapy.komok.tech.time.TimeSourceModule
import java.security.SecureRandom

@Module
open class TimeBasedOneTimePasswordModule(
    private val base32Module: Base32Module,
    private val timeSourceModule: TimeSourceModule,
) {
    open val timeBasedOneTimePasswordService by lazy {
        TimeBasedOneTimePasswordService(
            base32 = base32Module.base32,
            timeSource = timeSourceModule.timeSource,
        )
    }

    open val random by lazy {
        SecureRandom()
    }

    open val generateTimeBasedOneTimeKeyService by lazy {
        GenerateTimeBasedOneTimeKeyService(
            base32 = base32Module.base32,
            random = random,
        )
    }
}
