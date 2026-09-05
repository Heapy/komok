package io.heapy.komok.infra.base32

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class Base32Module {
    context(_: KomokContext)
    open suspend fun base32(): Base32 =
        Base32()
}
