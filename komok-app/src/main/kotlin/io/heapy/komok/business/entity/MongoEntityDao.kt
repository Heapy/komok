package io.heapy.komok.business.entity

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.tech.logging.Logger
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoEntityDao(
    private val database: MongoDatabase
) {
    context(UserContext)
    suspend fun insertPost(
        title: String,
        text: String,
        date: String,
        readStatus: Boolean,
    ): String? {
        return database.getCollection<Document>("entity")
            .insertOne(
                Document(
                    mapOf(
                        "title" to title,
                        "text" to text,
                        "date" to date,
                        "read_status" to readStatus,
                        "user_id" to user.id,
                    )
                )
            )
            .also {
                log.info("Inserted post: $it")
            }
            .let {
                it.insertedId?.asObjectId()?.value?.toString()
            }
    }

    /**
     * Get latest events for homepage feed.
     */
    context(UserContext)
    suspend fun getLatest(
        limit: Int = 100,
        offset: Int = 0,
    ): List<String> {
        return database.getCollection<Document>("entity")
            .find(
                Document(
                    mapOf(
                        "read_status" to false,
                        "user_id" to user.id,
                    )
                )
            )
            .sort(
                Document(
                    mapOf(
                        "date" to -1,
                    )
                )
            )
            .limit(limit)
            .skip(offset)
            .toList()
            .map {
                it.toJson()
            }
    }

    context(UserContext)
    suspend fun markPostAsRead(postId: String): Long {
        return database.getCollection<Document>("entity")
            .updateOne(
                Document(
                    mapOf(
                        "_id" to ObjectId(postId),
                        "user_id" to user.id,
                    )
                ),
                Document(
                    mapOf(
                        "\$set" to mapOf(
                            "read_status" to true,
                        )
                    )
                )
            )
            .also {
                log.info("Marked post as read: $it")
            }
            .let {
                it.matchedCount
            }
    }

    private companion object : Logger()
}
