package io.heapy.komok.dao.mg

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.heapy.kotomi.config.ConfigurationModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class MongoModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KotomiContext)
    suspend fun client(): MongoClient {
        val mc = mongoConfiguration()
        return MongoClient.create("mongodb://${mc.username}:${mc.password}@${mc.host}:${mc.port}")
    }

    context(_: KotomiContext)
    suspend fun komokDatabase(): MongoDatabase =
        client().getDatabase("komok")

    context(_: KotomiContext)
    suspend fun mongoConfiguration(): MongoConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = MongoConfiguration.serializer(),
                path = "mongo",
            )
}
