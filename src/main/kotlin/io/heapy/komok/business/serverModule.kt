@file:OptIn(ExperimentalSerializationApi::class)

package io.heapy.komok.business

import com.typesafe.config.Config
import io.heapy.komok.configuration.configModule
import io.heapy.komok.infra.di.module
import io.heapy.komok.infra.di.provide
import io.heapy.komok.metrics.metricsModule
import io.heapy.komok.business.routes.person
import io.heapy.komok.business.routes.ping
import io.heapy.komok.store.databaseModule
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import org.jooq.DSLContext
import java.io.File

val serverModule by module {
    dependency(configModule)
    dependency(databaseModule)
    dependency(metricsModule)
    provide(::serverConfig)
    provide(::jwtConfig)
    provide(::server)
}

fun server(
    serverConfig: ServerConfiguration,
    jwtConfig: JwtConfiguration,
    dslContext: DSLContext,
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

            ping()
            person(dslContext)

            authenticate("jwt") {
            }
        }
        configureWebSockets()
        configureMonitoring(meterRegistry)
    }
}

fun serverConfig(config: Config): ServerConfiguration =
    Hocon.decodeFromConfig(config.getConfig("server"))

fun jwtConfig(config: Config): JwtConfiguration =
    Hocon.decodeFromConfig(config.getConfig("jwt"))
