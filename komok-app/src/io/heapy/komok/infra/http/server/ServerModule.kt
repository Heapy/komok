package io.heapy.komok.infra.http.server

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing

@Module
open class ServerModule(
    private val serverRoutingConfigurationModule: ServerRoutingConfigurationModule,
    private val serverApplicationConfigurationModule: ServerApplicationConfigurationModule,
    private val serverConfigurationModule: ServerConfigurationModule,
) {
    context(_: KomokContext)
    open suspend fun server(): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
        val config = serverConfigurationModule.config()
        val features = serverApplicationConfigurationModule.features()
        val routes = serverRoutingConfigurationModule.routes()

        return embeddedServer(
            factory = CIO,
            port = config.port,
            host = config.host,
        ) {
            features.run {
                install()
            }

            routing {
                routes.run {
                    install()
                }
            }
        }
    }
}
