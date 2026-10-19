package io.heapy.komok.tech.runtime.providershape

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

class ShapeService(val value: String)

@Module
class ProviderShapeModule {
    context(_: KomokContext)
    suspend fun service(): ShapeService =
        ShapeService(
            value = listOf(
                privateHelper(),
                publicHelperWithValueParam("param"),
                nonSuspendHelper(),
            ).joinToString(":"),
        )

    context(_: KomokContext)
    private suspend fun privateHelper(): String =
        "private"

    context(_: KomokContext)
    suspend fun publicHelperWithValueParam(value: String): String =
        value

    context(_: KomokContext)
    fun nonSuspendHelper(): String =
        "non-suspend"

    context(_: KomokContext)
    suspend fun unitHelper() {
        // Unit-returning helpers are not provider bindings.
    }
}
