package io.heapy.komok.tech.runtime.providershape

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

class ShapeService(val value: String)

@Module
open class ProviderShapeModule {
    context(_: KomokContext)
    open suspend fun service(): ShapeService =
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
    open suspend fun publicHelperWithValueParam(value: String): String =
        value

    context(_: KomokContext)
    open fun nonSuspendHelper(): String =
        "non-suspend"

    context(_: KomokContext)
    open suspend fun unitHelper() {
        // Unit-returning helpers are not provider bindings.
    }
}
