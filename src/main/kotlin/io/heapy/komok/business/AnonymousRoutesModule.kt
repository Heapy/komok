package io.heapy.komok.business

import io.heapy.komok.business.healthcheck.HealthCheckRoute
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class AnonymousRoutesModule {
    open val anonymousRoutes: KomokRoutes by lazy {
        KomokRoutes(
            routes = listOf(
                healthCheckRoute,
            ),
        )
    }

    private val healthCheckRoute by lazy {
        HealthCheckRoute()
    }
}
