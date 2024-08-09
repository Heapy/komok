package io.heapy.komok.business.user.session

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.heapy.komok.dao.mg.MongoV1.Session
import org.bson.types.ObjectId
import kotlin.time.Duration.Companion.hours

class UserSession(
    private val database: MongoDatabase,
) {
    suspend fun createSession(
        userId: ObjectId,
    ) {
        database.getCollection<Session>(Session.COLLECTION)
            .insertOne(
                Session(
                    id = ObjectId(),
                    userId = userId,
                    expiration = java.time.Instant.now().epochSecond + sessionLifetime.inWholeSeconds,
                    ip = "",
                    device = "",
                )
            )
    }

    companion object {
        val sessionLifetime = 1.hours
    }
}
