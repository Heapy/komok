@file:JvmName("Application")
@file:Suppress("FunctionName")

package io.heapy.vipassana

import io.heapy.vipassana.config.Dotenv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.nio.file.Path

fun main() {
    println("Starting application")
    ApplicationFactory().run()
}

open class ApplicationFactory {
    open val postgresConfiguration by lazy {
        PostgresConfigurationProvider(
            dotenv = dotenv,
        )
    }

    open val dotenv by lazy {
        Dotenv.new()
    }

//    open val hikariConfig: HikariConfig by lazy {
//        HikariConfig().apply {
//            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
//            username = jdbcConfiguration.user
//            password = jdbcConfiguration.password
//            dataSourceProperties["databaseName"] = jdbcConfiguration.database
//            dataSourceProperties["serverName"] = jdbcConfiguration.server
//            dataSourceProperties["portNumber"] = jdbcConfiguration.port
//
//        }
//    }
//
//    open val hikariDataSource: HikariDataSource by lazy {
//        HikariDataSource(hikariConfig)
//    }

    open val serverConfiguration by lazy {
        ServerConfigurationProvider(
            dotenv = dotenv,
        )
    }

    open fun run() {
        LOGGER.info("Application started. JVM running for ${uptime}ms")
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(ApplicationFactory::class.java)
    }
}

private inline val uptime: Long
    get() = ManagementFactory.getRuntimeMXBean().uptime

data class ServerConfiguration(
    val port: Int,
    val host: String,
    val resources: Path,
)

fun ServerConfigurationProvider(dotenv: Dotenv) =
    ServerConfiguration(
        port = dotenv["VIPASSANA_SERVER_PORT"]?.toInt() ?: 9556,
        host = dotenv["VIPASSANA_SERVER_HOST"] ?: "127.0.0.1",
        resources = dotenv
            .getOrDefault("VIPASSANA_SERVER_RESOURCES", "/frontend")
            .let(Path::of),
    )

data class PostgresConfiguration(
    val user: String,
    val password: String,
    val database: String,
    val host: String,
    val port: Int,
)

fun PostgresConfigurationProvider(dotenv: Dotenv) =
    PostgresConfiguration(
        user = dotenv["VIPASSANA_POSTGRES_USER"] ?: "vipassana",
        password = dotenv["VIPASSANA_POSTGRES_PASSWORD"] ?: "vipassana",
        database = dotenv["VIPASSANA_POSTGRES_DATABASE"] ?: "vipassana",
        host = dotenv["VIPASSANA_POSTGRES_HOST"] ?: "127.0.0.1",
        port = dotenv["VIPASSANA_POSTGRES_PORT"]?.toInt() ?: 9557,
    )
