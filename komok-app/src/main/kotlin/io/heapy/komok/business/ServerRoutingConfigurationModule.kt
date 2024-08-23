package io.heapy.komok.business

import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class ServerRoutingConfigurationModule(
    private val serverConfigurationModule: ServerConfigurationModule,
    private val anonymousRoutesModule: AnonymousRoutesModule,
    private val authenticatedRoutesModule: AuthenticatedRoutesModule,
) {
    open val routes: KomokRoutes by lazy {
        KomokRoutes(
            listOf(
                staticFilesRoute,
                anonymousRoutesModule.anonymousRoutes,
                AuthenticatedRoutes(
                    routes = authenticatedRoutesModule.authenticatedRoutes,
                ),
            ),
        )
    }

    open val staticFilesRoute by lazy {
        StaticFilesRoute(
            resources = serverConfigurationModule.config.resources,
        )
    }
}
