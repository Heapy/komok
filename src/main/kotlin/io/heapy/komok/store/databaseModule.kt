@file:OptIn(ExperimentalSerializationApi::class)

package io.heapy.komok.store

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.heapy.komok.configuration.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.ds.PGSimpleDataSource

@Module
open class DatabaseModule(
    private val configurationModule: ConfigurationModule,
) {
    open val postgresConfig by lazy {
        postgresConfig(configurationModule.config)
    }

    open val hikariConfig by lazy {
        hikariConfig(postgresConfig)
    }

    open val hikariDataSource by lazy {
        hikariDataSource(hikariConfig)
    }

    open val dslContext by lazy {
        dslContext(hikariDataSource)
    }
}

fun postgresConfig(config: Config): PostgresConfiguration =
    Hocon.decodeFromConfig(config.getConfig("jdbc"))

fun hikariConfig(
    postgresConfig: PostgresConfiguration,
): HikariConfig {
    return HikariConfig().apply {
        dataSourceClassName = PGSimpleDataSource::class.qualifiedName
        username = postgresConfig.user
        password = postgresConfig.password
        dataSourceProperties["databaseName"] = postgresConfig.database
        dataSourceProperties["serverName"] = postgresConfig.host
        dataSourceProperties["portNumber"] = postgresConfig.port
    }
}

fun hikariDataSource(
    hikariConfig: HikariConfig,
): HikariDataSource {
    return HikariDataSource(hikariConfig)
}

fun dslContext(
    hikariDataSource: HikariDataSource,
): DSLContext {
    System.setProperty(
        "org.jooq.no-logo",
        "true",
    )
    System.setProperty(
        "org.jooq.no-tips",
        "true",
    )
    return DSL.using(
        hikariDataSource,
        SQLDialect.POSTGRES,
    )
}
