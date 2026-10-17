package io.heapy.komok.dao.mg

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class MongoModule(
    private val configurationModule: ConfigurationModule,
) {
    open val client by lazy {
        val mc = mongoConfiguration
        MongoClient.create("mongodb://${mc.username}:${mc.password}@${mc.host}:${mc.port}")
    }

    open val komokDatabase by lazy {
        client.getDatabase("komok")
    }

    open val mongoConfiguration: MongoConfiguration by lazy {
        configurationModule
            .config
            .read(
                deserializer = MongoConfiguration.serializer(),
                path = "mongo",
            )
    }
}
