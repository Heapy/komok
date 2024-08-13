package io.heapy.komok.dao.pg

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.postgresql.ds.PGSimpleDataSource

@Module
open class DatabaseModule(
    private val configurationModule: ConfigurationModule,
) {
    open val postgresConfig: PostgresConfiguration by lazy {
        configurationModule
            .config
            .read(
                PostgresConfiguration.serializer(),
                "jdbc",
            )
    }

    open val hikariConfig by lazy {
        HikariConfig().apply {
            dataSourceClassName = PGSimpleDataSource::class.qualifiedName
            username = postgresConfig.user
            password = postgresConfig.password
            dataSourceProperties["databaseName"] = postgresConfig.database
            dataSourceProperties["serverName"] = postgresConfig.host
            dataSourceProperties["portNumber"] = postgresConfig.port
        }
    }

    open val hikariDataSource by lazy {
        HikariDataSource(hikariConfig)
    }

    open val dslContext by lazy {
        System.setProperty(
            "org.jooq.no-logo",
            "true",
        )
        System.setProperty(
            "org.jooq.no-tips",
            "true",
        )
        DSL.using(
            hikariDataSource,
            SQLDialect.POSTGRES,
        )
    }
}
