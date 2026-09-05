package io.heapy.komok.dao.mg

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class MongoModule(
    private val configurationModule: ConfigurationModule,
) {
    context(_: KomokContext)
    open suspend fun client(): MongoClient {
        val mc = mongoConfiguration()
        return MongoClient.create("mongodb://${mc.username}:${mc.password}@${mc.host}:${mc.port}")
    }

    context(_: KomokContext)
    open suspend fun komokDatabase(): MongoDatabase =
        client().getDatabase("komok")

    context(_: KomokContext)
    open suspend fun mongoConfiguration(): MongoConfiguration =
        configurationModule
            .config()
            .read(
                deserializer = MongoConfiguration.serializer(),
                path = "mongo",
            )
}
