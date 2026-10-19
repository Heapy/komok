package io.heapy.komok.business.entity

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.server.common.KomokRoutes
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class EntityModule(
    private val mongoModule: MongoModule,
) {
    context(_: KotomiContext)
    suspend fun routes(): KomokRoutes =
        KomokRoutes(
            routes = listOf(
                entityInsertRoute(),
                getLatestUnreadRoute(),
                updateStatusRoute(),
            ),
        )

    context(_: KotomiContext)
    suspend fun entityDao(): MongoEntityDao =
        MongoEntityDao(
            database = mongoModule.komokDatabase(),
        )

    context(_: KotomiContext)
    suspend fun entityInsertRoute(): MongoEntityInsertRoute =
        MongoEntityInsertRoute(
            mongoEntityDao = entityDao(),
        )

    context(_: KotomiContext)
    suspend fun getLatestUnreadRoute(): MongoGetLatestUnreadRoute =
        MongoGetLatestUnreadRoute(
            mongoEntityDao = entityDao(),
        )

    context(_: KotomiContext)
    suspend fun updateStatusRoute(): MongoUpdateStatusRoute =
        MongoUpdateStatusRoute(
            mongoEntityDao = entityDao(),
        )
}
