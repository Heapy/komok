@file:JvmName("Application")
@file:Suppress("FunctionName")

package io.heapy.vipassana

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.heapy.vipassana.configuration.Env
import io.heapy.vipassana.database.HikariConnectionProvider
import io.heapy.vipassana.logger.Logger
import org.postgresql.ds.PGSimpleDataSource
import java.lang.management.ManagementFactory
import java.nio.file.Path

fun main() {
    ApplicationFactory().run()
}

open class ApplicationFactory {
    open val postgresConfiguration by lazy {
        PostgresConfigurationProvider(
            env = env,
        )
    }

    open val env by lazy {
        Env.new()
    }

    open val hikariConfig: HikariConfig by lazy {
        HikariConfig().apply {
            dataSourceClassName = PGSimpleDataSource::class.qualifiedName
            username = postgresConfiguration.user
            password = postgresConfiguration.password
            dataSourceProperties["databaseName"] = postgresConfiguration.database
            dataSourceProperties["serverName"] = postgresConfiguration.host
            dataSourceProperties["portNumber"] = postgresConfiguration.port
        }
    }

    open val hikariDataSource: HikariDataSource by lazy {
        HikariDataSource(hikariConfig)
    }

    open val hikariConnectionProvider: HikariConnectionProvider by lazy {
        HikariConnectionProvider(
            hikariDataSource = hikariDataSource,
        )
    }

    open val serverConfiguration by lazy {
        ServerConfigurationProvider(
            env = env,
        )
    }

    open fun run() {
        log.info("Application started. JVM running for ${uptime}ms")
    }

    companion object {
        private val log = Logger<ApplicationFactory>()
    }
}

private inline val uptime: Long
    get() = ManagementFactory.getRuntimeMXBean().uptime

data class ServerConfiguration(
    val port: Int,
    val host: String,
    val resources: Path,
)

fun ServerConfigurationProvider(env: Env) =
    ServerConfiguration(
        port = env.getOrDefault("VIPASSANA_SERVER_PORT", String::toInt, 9556),
        host = env.getOrDefault("VIPASSANA_SERVER_HOST", "127.0.0.1"),
        resources = env
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

fun PostgresConfigurationProvider(env: Env) =
    PostgresConfiguration(
        user = env.getOrDefault("VIPASSANA_POSTGRES_USER", "vipassana"),
        password = env.getOrDefault("VIPASSANA_POSTGRES_PASSWORD", "vipassana"),
        database = env.getOrDefault("VIPASSANA_POSTGRES_DATABASE", "vipassana"),
        host = env.getOrDefault("VIPASSANA_POSTGRES_HOST", "127.0.0.1"),
        port = env.getOrDefault("VIPASSANA_POSTGRES_PORT", String::toInt, 9557),
    )
