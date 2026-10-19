package io.heapy.komok.tech.runtime.allopen

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

/** Final classes and methods retain their JVM shape while IR instruments provider bodies. */

class Setting(val value: String)

class Service(val setting: String)

@Module
class NoOpenModule {
    context(_: KomokContext)
    suspend fun setting(): Setting =
        Setting("configured")

    context(_: KomokContext)
    suspend fun service(): Service =
        Service(setting().value)
}
