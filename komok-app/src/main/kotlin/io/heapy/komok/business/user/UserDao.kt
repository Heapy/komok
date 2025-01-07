package io.heapy.komok.business.user

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.heapy.komok.dao.mg.MongoV1.User
import io.heapy.komok.tech.logging.Logger
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId

class UserDao(
    private val database: MongoDatabase,
) {
    /**
     * Insert user, return id.
     */
    suspend fun insertUser(
        email: String,
        hash: String,
    ): String {
        val objectId = ObjectId()
        database.getCollection<User>(User.COLLECTION)
            .insertOne(
                User(
                    id = objectId,
                    email = email,
                    hash = hash,
                    authenticatorKey = "",
                )
            )
            .also { result ->
                log.info("User insert: {} acknowledged: {}", email, result.wasAcknowledged())
            }

        return objectId.toHexString()
    }

    /**
     * Get user by email, return null if not found.
     */
    suspend fun getUser(
        email: String,
    ): User? {
        return database
            .getCollection<User>(User.COLLECTION)
            .find(
                Filters.eq(
                    User::email.name,
                    email,
                )
            )
            .firstOrNull()
    }

    private companion object : Logger()
}
