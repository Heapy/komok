package io.heapy.komok.dao.mg

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

object MongoV1 {
    data class User(
        @param:BsonId val id: ObjectId,
        val email: String,
        val hash: String,
        val authenticatorKey: String,
    ) {
        companion object {
            const val COLLECTION = "user"
        }
    }

    data class Session(
        @param:BsonId val id: ObjectId,
        val userId: ObjectId,
        val expiration: Long,
        val ip: String,
        val device: String,
        val token: String,
    ) {
        companion object {
            const val COLLECTION = "session"
        }
    }
}
