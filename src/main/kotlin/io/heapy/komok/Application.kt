@file:JvmName("Application")
@file:Suppress("FunctionName")
@file:OptIn(ExperimentalSerializationApi::class)

package io.heapy.komok

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.heapy.komok.database.PostgresConfiguration
import io.heapy.komok.server.*
import io.heapy.komok.server.routes.person
import io.heapy.komok.server.routes.ping
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.json.Json
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.ds.PGSimpleDataSource
import java.io.File
import java.lang.management.ManagementFactory
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.server.cio.CIO as ServerCIO

fun main() {
    ApplicationFactory().run()
}

open class ApplicationFactory {
    open val config: Config by lazy {
        ConfigFactory.load()
    }

    open val postgresConfig: PostgresConfiguration by lazy {
        Hocon.decodeFromConfig(config.getConfig("jdbc"))
    }

    open val httpClient by lazy {
        HttpClient(ClientCIO) {
            install(ContentNegotiation) {
                json(json = Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    open val serverConfig: ServerConfiguration by lazy {
        Hocon.decodeFromConfig(config.getConfig("server"))
    }

    open val jwtConfig: JwtConfiguration by lazy {
        Hocon.decodeFromConfig(config.getConfig("jwt"))
    }

    open val dslContext: DSLContext by lazy {
        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")
        DSL.using(hikariDataSource, SQLDialect.POSTGRES)
    }

    open val server by lazy {
        embeddedServer(
            factory = ServerCIO,
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

    open val meterRegistry by lazy {
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    open val hikariConfig: HikariConfig by lazy {
        HikariConfig().apply {
            dataSourceClassName = PGSimpleDataSource::class.qualifiedName
            username = postgresConfig.user
            password = postgresConfig.password
            dataSourceProperties["databaseName"] = postgresConfig.database
            dataSourceProperties["serverName"] = postgresConfig.host
            dataSourceProperties["portNumber"] = postgresConfig.port
        }
    }

    open val hikariDataSource: HikariDataSource by lazy {
        HikariDataSource(hikariConfig)
    }

    open fun run() {
        server.start(wait = true)
        log.info("Application started. JVM running for ${uptime}ms")
    }

    companion object {
        private val log = logger {}
    }
}

private inline val uptime: Long
    get() = ManagementFactory.getRuntimeMXBean().uptime
