package io.heapy.komok.business

import io.heapy.komok.tech.di.lib.Module
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing

@Module
open class ServerModule(
    private val serverRoutingConfigurationModule: ServerRoutingConfigurationModule,
    private val serverApplicationConfigurationModule: ServerApplicationConfigurationModule,
    private val serverConfigurationModule: ServerConfigurationModule,
) {
    open val server by lazy {
        embeddedServer(
            factory = CIO,
            port = serverConfigurationModule.config.port,
            host = serverConfigurationModule.config.host,
        ) {
            serverApplicationConfigurationModule.features.run {
                install()
            }

            routing {
                serverRoutingConfigurationModule.routes.run {
                    install()
                }
            }
        }
    }
}
