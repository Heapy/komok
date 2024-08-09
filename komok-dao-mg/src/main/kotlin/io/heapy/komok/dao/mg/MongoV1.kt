package io.heapy.komok.dao.mg

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

object MongoV1 {
    data class User(
        @BsonId val id: ObjectId,
        val email: String,
        val password: String,
        val authenticatorKey: String,
    ) {
        companion object {
            const val COLLECTION = "user"
        }
    }

    data class Session(
        @BsonId val id: ObjectId,
        val userId: ObjectId,
        val expiration: Long,
        val ip: String,
        val device: String,
    ) {
        companion object {
            const val COLLECTION = "session"
        }
    }
}
