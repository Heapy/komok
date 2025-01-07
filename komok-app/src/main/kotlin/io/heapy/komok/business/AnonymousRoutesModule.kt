package io.heapy.komok.business

import io.heapy.komok.business.healthcheck.HealthCheckRoute
import io.heapy.komok.business.login.LoginModule
import io.heapy.komok.server.common.KomokRoute
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class AnonymousRoutesModule(
    private val loginModule: LoginModule,
) {
    open val anonymousRoutes: KomokRoute by lazy {
        KomokRoutes(
            routes = listOf(
                healthCheckRoute,
                loginModule.loginRoute,
            ),
        )
    }

    private val healthCheckRoute by lazy {
        HealthCheckRoute()
    }
}
