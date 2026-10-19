package io.heapy.komok.business

import io.heapy.komok.business.healthcheck.HealthCheckRoute
import io.heapy.komok.business.login.LoginModule
import io.heapy.komok.server.common.KomokRoute
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class AnonymousRoutesModule(
    private val loginModule: LoginModule,
) {
    context(_: KomokContext)
    suspend fun anonymousRoutes(): KomokRoute =
        KomokRoutes(
            routes = listOf(
                healthCheckRoute(),
                loginModule.loginRoute(),
            ),
        )

    private fun healthCheckRoute(): HealthCheckRoute =
        HealthCheckRoute()
}
