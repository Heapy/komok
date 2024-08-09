package io.heapy.komok.dao.pg

import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class PostgresEntityModule(
    private val databaseModule: DatabaseModule,
) {
    open val entityDao by lazy {
        EntityDao()
    }

    open val routes by lazy {
        KomokRoutes(
            routes = listOf(
                entityInsertRoute,
                getLatestUnreadRoute,
                updateStatusRoute,
            )
        )
    }

    open val entityInsertRoute by lazy {
        EntityInsertRoute(
            dslContext = databaseModule.dslContext,
            entityDao = entityDao,
        )
    }

    open val getLatestUnreadRoute by lazy {
        GetLatestUnreadRoute(
            dslContext = databaseModule.dslContext,
            entityDao = entityDao,
        )
    }

    open val updateStatusRoute by lazy {
        UpdateStatusRoute(
            dslContext = databaseModule.dslContext,
            entityDao = entityDao,
        )
    }
}
