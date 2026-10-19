package io.heapy.komok.infra.http.server

import io.heapy.komok.business.AnonymousRoutesModule
import io.heapy.komok.business.AuthenticatedRoutesModule
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class ServerRoutingConfigurationModule(
    private val serverConfigurationModule: ServerConfigurationModule,
    private val anonymousRoutesModule: AnonymousRoutesModule,
    private val authenticatedRoutesModule: AuthenticatedRoutesModule,
) {
    context(_: KomokContext)
    suspend fun routes(): KomokRoutes =
        KomokRoutes(
            listOf(
                staticFilesRoute(),
                anonymousRoutesModule.anonymousRoutes(),
                authenticatedRoutesModule.authenticatedRoutes(),
            ),
        )

    context(_: KomokContext)
    suspend fun staticFilesRoute(): StaticFilesRoute =
        StaticFilesRoute(
            resources = serverConfigurationModule.config().resources,
        )
}
