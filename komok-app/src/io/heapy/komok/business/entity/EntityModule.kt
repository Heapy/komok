package io.heapy.komok.business.entity

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class EntityModule(
    private val mongoModule: MongoModule,
) {
    context(_: KomokContext)
    suspend fun routes(): KomokRoutes =
        KomokRoutes(
            routes = listOf(
                entityInsertRoute(),
                getLatestUnreadRoute(),
                updateStatusRoute(),
            ),
        )

    context(_: KomokContext)
    suspend fun entityDao(): MongoEntityDao =
        MongoEntityDao(
            database = mongoModule.komokDatabase(),
        )

    context(_: KomokContext)
    suspend fun entityInsertRoute(): MongoEntityInsertRoute =
        MongoEntityInsertRoute(
            mongoEntityDao = entityDao(),
        )

    context(_: KomokContext)
    suspend fun getLatestUnreadRoute(): MongoGetLatestUnreadRoute =
        MongoGetLatestUnreadRoute(
            mongoEntityDao = entityDao(),
        )

    context(_: KomokContext)
    suspend fun updateStatusRoute(): MongoUpdateStatusRoute =
        MongoUpdateStatusRoute(
            mongoEntityDao = entityDao(),
        )
}
