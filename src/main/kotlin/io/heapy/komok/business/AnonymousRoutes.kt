package io.heapy.komok.business

import io.heapy.komok.business.healthcheck.HealthCheckRoute
import io.heapy.komok.infra.server.KomokRoute
import io.heapy.komok.tech.di.lib.Module
import io.ktor.server.routing.Route

interface AnonymousRoutes : KomokRoute

class DefaultAnonymousRoutes(
    private val healthCheckRoute: HealthCheckRoute,
) : AnonymousRoutes {
    override fun Route.install() {
        healthCheckRoute.run { install() }
    }
}

@Module
open class AnonymousRoutesModule {
    open val anonymousRoutes by lazy {
        DefaultAnonymousRoutes(
            healthCheckRoute = healthCheckRoute,
        )
    }

    private val healthCheckRoute by lazy {
        HealthCheckRoute()
    }
}
