package io.heapy.komok.dao.pg

import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.database.tables.pojos.Entity
import io.heapy.komok.database.tables.references.ENTITY
import io.heapy.komok.logging.Logger
import org.jooq.JSONB
import org.jooq.impl.DSL

class EntityDao {
    context(UserContext, TransactionContext)
    suspend fun insertPost(
        title: String,
        text: String,
        date: String,
        readStatus: Boolean,
    ): Long? = dslContext {
        val jsonData = JSONB.jsonb(
            """{
                "title": "$title",
                "text": "$text",
                "date": "$date",
                "read_status": $readStatus,
                "user_id": "${user.id}"
            }""".trimMargin(),
        )

        dslContext
            .insertInto(ENTITY)
            .set(
                ENTITY.DATA,
                jsonData,
            )
            .returningResult(ENTITY.ID)
            .fetchOneInto(Long::class.java)
    }

    /**
     * Get latest events for homepage feed.
     */
    context(UserContext, TransactionContext)
    suspend fun getLatest(
        limit: Int = 100,
        offset: Int = 0,
    ): List<Entity> = dslContext {
        dslContext
            .select(
                ENTITY.ID,
                ENTITY.DATA,
            )
            .from(ENTITY)
            .where(
                DSL
                    .field("(data->>'read_status')::boolean")
                    .eq(false),
            )
            .orderBy(
                DSL
                    .field("data->>'date'")
                    .desc(),
            )
            .limit(limit)
            .offset(offset)
            .also {
                log.info("SQL: {}", it.sql)
            }
            .fetchInto(Entity::class.java)
    }

    context(UserContext, TransactionContext)
    suspend fun markPostAsRead(postId: Long): Int = dslContext {
        dslContext
            .update(ENTITY)
            .set(
                ENTITY.DATA,
                DSL.field(
                    "jsonb_set({0}, '{read_status}', {1}::jsonb)",
                    JSONB::class.java,
                    ENTITY.DATA,
                    DSL.inline("true"),
                ),
            )
            .where(ENTITY.ID.eq(postId))
            .execute()
    }

    private companion object : Logger()
}
