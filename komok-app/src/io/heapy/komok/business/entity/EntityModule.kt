package io.heapy.komok.business.entity

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.di.lib.Module

@Module
open class EntityModule(
    private val mongoModule: MongoModule,
) {
    open val routes by lazy {
        KomokRoutes(
            routes = listOf(
                entityInsertRoute,
                getLatestUnreadRoute,
                updateStatusRoute,
            ),
        )
    }

    open val entityDao by lazy {
        MongoEntityDao(
            database = mongoModule.komokDatabase,
        )
    }

    open val entityInsertRoute by lazy {
        MongoEntityInsertRoute(
            mongoEntityDao = entityDao,
        )
    }

    open val getLatestUnreadRoute by lazy {
        MongoGetLatestUnreadRoute(
            mongoEntityDao = entityDao,
        )
    }

    open val updateStatusRoute by lazy {
        MongoUpdateStatusRoute(
            mongoEntityDao = entityDao,
        )
    }
}
