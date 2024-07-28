@file:OptIn(ExperimentalSerializationApi::class)

package io.heapy.komok.business

import io.heapy.komok.business.login.JwtConfiguration
import io.heapy.komok.configuration.ConfigurationModule
import io.heapy.komok.metrics.MetricsModule
import io.heapy.komok.tech.di.lib.Module
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import java.io.File

@Module
open class ServerModule(
    private val configModule: ConfigurationModule,
    private val anonymousRoutesModule: AnonymousRoutesModule,
    private val authenticatedRoutesModule: AuthenticatedRoutesModule,
    private val metricsModule: MetricsModule,
) {
    open val serverConfig: ServerConfiguration by lazy {
        Hocon.decodeFromConfig(configModule.config.getConfig("server"))
    }

    open val jwtConfig: JwtConfiguration by lazy {
        Hocon.decodeFromConfig(configModule.config.getConfig("jwt"))
    }

    open val server by lazy {
        server(
            anonymousRoutesModule.anonymousRoutes,
            authenticatedRoutesModule.authenticatedRoutes,
            serverConfig,
            jwtConfig,
            metricsModule.meterRegistry,
        )
    }
}

fun server(
    anonymousRoutes: AnonymousRoutes,
    authenticatedRoutes: AuthenticatedRoutes,
    serverConfig: ServerConfiguration,
    jwtConfig: JwtConfiguration,
    meterRegistry: PrometheusMeterRegistry,
): ApplicationEngine {
    return embeddedServer(
        factory = CIO,
        port = serverConfig.port,
        host = serverConfig.host,
    ) {
        defaults(jwtConfig)

        routing {
            staticFiles("/", File(serverConfig.resources)) {
                preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)
                enableAutoHeadResponse()
            }

            anonymousRoutes.run {
                install()
            }

            route("/api") {
                authenticatedRoutes.run {
                    install()
                }
            }

            authenticate("jwt") {

            }
        }
        configureWebSockets()
        configureMonitoring(meterRegistry)
    }
}
