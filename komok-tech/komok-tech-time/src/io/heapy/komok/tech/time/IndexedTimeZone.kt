package io.heapy.komok.tech.time

import io.heapy.komok.tech.runtime.IntoSet
import io.heapy.komok.tech.runtime.Service

data class IndexedTimeZone(
    val id: String,
)

@Service
fun indexedTimeZone(): IndexedTimeZone =
    IndexedTimeZone("UTC")

interface TimeCodec {
    val name: String
}

private data class NamedTimeCodec(
    override val name: String,
) : TimeCodec

@Service
@IntoSet
fun zoneTimeCodec(): TimeCodec =
    NamedTimeCodec("zone")
