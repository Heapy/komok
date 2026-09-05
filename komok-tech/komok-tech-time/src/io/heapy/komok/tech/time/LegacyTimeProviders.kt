package io.heapy.komok.tech.time

data class ProvidersFromTimeZone(
    val id: String,
)

object LegacyTimeProviders {
    fun providersFromTimeZone(): ProvidersFromTimeZone =
        ProvidersFromTimeZone("providers-from")
}
