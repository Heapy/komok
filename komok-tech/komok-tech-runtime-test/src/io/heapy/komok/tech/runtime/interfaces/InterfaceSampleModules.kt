package io.heapy.komok.tech.runtime.interfaces

import io.heapy.komok.tech.runtime.IntoSet
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

/**
 * Interface-module sample: no-arg default methods are leaf providers and methods with
 * value parameters are param-wired by exact return type.
 */
@Module
interface InterfaceLeafModule {
    context(_: KomokContext)
    suspend fun answer(): Int = 42
}

data class InterfaceHost(val value: String)
data class InterfacePort(val value: Int)
data class InterfaceEndpoint(val value: String)

@Module
interface InterfaceParamModule {
    context(_: KomokContext)
    suspend fun host(): InterfaceHost =
        InterfaceHost("localhost")

    context(_: KomokContext)
    suspend fun port(): InterfacePort =
        InterfacePort(8080)

    context(_: KomokContext)
    suspend fun endpoint(
        host: InterfaceHost,
        port: InterfacePort,
    ): InterfaceEndpoint =
        InterfaceEndpoint("${host.value}:${port.value}")
}

interface InterfaceCodec {
    val name: String
}

data class InterfaceNamedCodec(
    override val name: String,
) : InterfaceCodec

data class InterfaceCodecRegistry(
    val codecs: Set<InterfaceCodec>,
) {
    fun names(): List<String> =
        codecs.map { it.name }
}

@Module
interface InterfaceSetModule {
    @IntoSet
    context(_: KomokContext)
    suspend fun jsonInterfaceCodec(): InterfaceCodec =
        InterfaceNamedCodec("json")

    @IntoSet
    context(_: KomokContext)
    suspend fun xmlInterfaceCodec(): InterfaceCodec =
        InterfaceNamedCodec("xml")

    context(_: KomokContext)
    suspend fun interfaceCodecRegistry(
        codecs: Set<InterfaceCodec>,
    ): InterfaceCodecRegistry =
        InterfaceCodecRegistry(codecs)
}
