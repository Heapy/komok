package io.heapy.komok.dao.pg

import io.heapy.komok.auth.common.User
import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jooq.DSLContext

class GetLatestUnreadRoute(
    private val entityDao: EntityDao,
    private val dslContext: DSLContext,
) : KomokRoute {
    @Serializable
    data class Response(
        val entities: List<EntityResponse>,
    )

    @Serializable
    data class EntityResponse(
        val id: Long,
        val data: String,
    )

    override fun Routing.install() {
        get("/entity") {
            val req = call.parameters["page"]?.toIntOrNull() ?: 0

            val entities = with(UserContext(User(id = "1"))) {
                with(TransactionContext(dslContext)) {
                    entityDao.getLatest(
                        limit = 100,
                        offset = 100 * req,
                    )
                }
            }

            call.respond(Response(entities = entities.map {
                EntityResponse(
                    id = it.id!!,
                    data = it.data.toString(),
                )
            }))
        }
    }
}
