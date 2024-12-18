package io.heapy.komok.infra.base32

import io.heapy.komok.tech.di.lib.Module

@Module
open class Base32Module {
    open val base32: Base32 by lazy {
        Base32()
    }
}
