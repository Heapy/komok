package io.heapy.komok.infra.base32

import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class Base32Module {
    context(_: KotomiContext)
    suspend fun base32(): Base32 =
        Base32()
}
