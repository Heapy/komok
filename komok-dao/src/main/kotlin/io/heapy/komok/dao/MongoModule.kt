package io.heapy.komok.dao

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.heapy.komok.tech.di.lib.Module

@Module
open class MongoModule {
    open val client by lazy {
        MongoClient.create("mongodb://komok:komok@localhost:27017")
    }

    open val komokDatabase by lazy {
        client.getDatabase("komok")
    }
}
