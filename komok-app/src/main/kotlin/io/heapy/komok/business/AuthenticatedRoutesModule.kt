package io.heapy.komok.business

import io.heapy.komok.business.entity.EntityModule
import io.heapy.komok.business.user.me.MeModule
import io.heapy.komok.infra.http.server.AuthenticatedRoutes
import io.heapy.komok.server.common.KomokRoute
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class AuthenticatedRoutesModule(
    private val entityModule: EntityModule,
    private val meModule: MeModule,
) {
    open val routes: KomokRoute by lazy {
        KomokRoutes(
            routes = listOf(
                meModule.meRoute,
                entityModule.routes,
            ),
        )
    }

    open val authenticatedRoutes: KomokRoute by lazy {
        AuthenticatedRoutes(
            routes = routes,
        )
    }
}
