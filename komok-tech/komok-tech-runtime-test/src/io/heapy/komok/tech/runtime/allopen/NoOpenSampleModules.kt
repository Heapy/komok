package io.heapy.komok.tech.runtime.allopen

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

/**
 * The intended user model: no `open` anywhere. The all-open plugin (configured on the
 * `@Module` annotation in this module's build) opens the class and its providers at compile
 * time, so the generated `NoOpenModule_Komok` subclass can override `setting()`/`service()`.
 *
 * KSP never inspects openness — it just emits the overrides; the compiler, with all-open
 * already applied, resolves overridability. The intra-module self-call `service() ->
 * setting()` further proves it: it records a binding edge only if it dispatched virtually,
 * which requires `setting()` to be overridable.
 */

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
