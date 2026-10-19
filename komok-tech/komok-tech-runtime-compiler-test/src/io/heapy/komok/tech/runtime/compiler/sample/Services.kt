package io.heapy.komok.tech.runtime.compiler.sample

import io.heapy.komok.tech.runtime.*

data class Host(val value: String)
data class Endpoint(val host: Host)

@Module
interface NetworkModule {
    context(_: KomokContext)
    suspend fun host(): Host = Host("localhost")

    context(_: KomokContext)
    suspend fun endpoint(host: Host): Endpoint = Endpoint(host)
}

data class Settings(val value: String)
@Service fun settings(): Settings = Settings("configured")
@Service class Client(val settings: Settings)

interface Codec { val name: String }
data class NamedCodec(override val name: String) : Codec
@Service @IntoSet fun json(): Codec = NamedCodec("json")
@Service @IntoSet fun xml(): Codec = NamedCodec("xml")
@Service class Codecs(val values: Set<Codec>)

@Service fun strings(): List<String> = listOf("string")
@Service fun numbers(): List<Int> = listOf(42)
@Service class GenericClient(val strings: List<String>, val numbers: List<Int>)

@Service class Events { val values = mutableListOf<String>() }
@Service class Dependency(val events: Events) {
    @Startup fun start() { events.values.add("dependency.start") }
    @Shutdown fun stop() { events.values.add("dependency.stop") }
}
@Service class Dependent(val dependency: Dependency, val events: Events) {
    @Startup fun start() { events.values.add("dependent.start") }
    @Shutdown fun stop() { events.values.add("dependent.stop") }
}
