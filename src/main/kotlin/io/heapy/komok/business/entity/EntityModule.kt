package io.heapy.komok.business.entity

import io.heapy.komok.dao.MongoModule
import io.heapy.komok.store.DatabaseModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class EntityModule(
    private val databaseModule: DatabaseModule,
    private val mongoModule: MongoModule,
) {
    open val entityDao by lazy {
        EntityDao()
    }

    open val mongoEntityDao by lazy {
        MongoEntityDao(
            database = mongoModule.komokDatabase,
        )
    }

    open val mongoEntityInsertRoute by lazy {
        MongoEntityInsertRoute(
            mongoEntityDao = mongoEntityDao,
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

    open val mongoGetLatestUnreadRoute by lazy {
        MongoGetLatestUnreadRoute(
            mongoEntityDao = mongoEntityDao,
        )
    }

    open val updateStatusRoute by lazy {
        UpdateStatusRoute(
            dslContext = databaseModule.dslContext,
            entityDao = entityDao,
        )
    }

    open val mongoUpdateStatusRoute by lazy {
        MongoUpdateStatusRoute(
            mongoEntityDao = mongoEntityDao,
        )
    }
}
