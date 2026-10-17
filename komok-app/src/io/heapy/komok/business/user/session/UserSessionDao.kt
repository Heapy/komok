package io.heapy.komok.business.user.session

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.heapy.komok.dao.mg.MongoV1.Session
import io.heapy.komok.infra.http.server.errors.AuthenticationError
import io.heapy.komok.infra.http.server.errors.authenticationError
import io.heapy.komok.tech.time.TimeSource
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId
import kotlin.time.Duration

class UserSessionDao(
    private val database: MongoDatabase,
    private val timeSource: TimeSource,
) {
    suspend fun createSession(
        userId: ObjectId,
        maxAge: Duration,
        ip: String,
        device: String,
        token: String,
    ) {
        database.getCollection<Session>(Session.COLLECTION)
            .insertOne(
                Session(
                    id = ObjectId(),
                    userId = userId,
                    expiration = timeSource.instant().epochSecond + maxAge.inWholeSeconds,
                    ip = ip,
                    device = device,
                    token = token,
                )
            )
    }

    suspend fun verifySession(
        token: String,
        ip: String,
    ): Session {
        val session = database
            .getCollection<Session>(Session.COLLECTION)
            .find(
                and(
                    eq(
                        Session::token.name,
                        token,
                    ),
                    eq(
                        Session::ip.name,
                        ip,
                    ),
                ),
            )
            .firstOrNull()

        if (session == null) {
            authenticationError(AuthenticationError.INVALID_SESSION)
        }

        if (session.expiration < timeSource.instant().epochSecond) {
            authenticationError(AuthenticationError.SESSION_EXPIRED)
        }

        return session
    }
}
