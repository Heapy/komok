package io.heapy.komok.infra.http.server

import io.heapy.komok.business.AnonymousRoutesModule
import io.heapy.komok.business.AuthenticatedRoutesModule
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class ServerRoutingConfigurationModule(
    private val serverConfigurationModule: ServerConfigurationModule,
    private val anonymousRoutesModule: AnonymousRoutesModule,
    private val authenticatedRoutesModule: AuthenticatedRoutesModule,
) {
    context(_: KotomiContext)
    suspend fun routes(): KomokRoutes =
        KomokRoutes(
            listOf(
                staticFilesRoute(),
                anonymousRoutesModule.anonymousRoutes(),
                authenticatedRoutesModule.authenticatedRoutes(),
            ),
        )

    context(_: KotomiContext)
    suspend fun staticFilesRoute(): StaticFilesRoute =
        StaticFilesRoute(
            resources = serverConfigurationModule.config().resources,
        )
}
