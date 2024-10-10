package io.heapy.komok.business

import io.heapy.komok.business.entity.EntityModule
import io.heapy.komok.business.user.UserRoute
import io.heapy.komok.infra.http.server.AuthenticatedRoutes
import io.heapy.komok.server.common.KomokRoute
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class AuthenticatedRoutesModule(
    private val entityModule: EntityModule,
) {
    open val userRoute by lazy {
        UserRoute()
    }

    open val routes: KomokRoute by lazy {
        KomokRoutes(
            routes = listOf(
                userRoute,
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
