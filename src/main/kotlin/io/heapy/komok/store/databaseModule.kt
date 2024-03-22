@file:OptIn(ExperimentalSerializationApi::class)

package io.heapy.komok.store

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.heapy.komok.configuration.configModule
import io.heapy.komok.infra.di.module
import io.heapy.komok.infra.di.provide
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.ds.PGSimpleDataSource
import java.util.concurrent.Executors

val databaseModule by module {
    dependency(configModule)
    provide(::postgresConfig)
    provide(::hikariConfig)
    provide(::hikariDataSource)
    provide(::dslContext)
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
        scheduledExecutor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().name("virtual-hikari-", 0).factory())
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
    System.setProperty("org.jooq.no-logo", "true")
    System.setProperty("org.jooq.no-tips", "true")
    return DSL.using(hikariDataSource, SQLDialect.POSTGRES)
}
