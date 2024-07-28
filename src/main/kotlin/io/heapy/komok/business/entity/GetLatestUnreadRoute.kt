package io.heapy.komok.business.entity

import io.heapy.komok.TransactionContext
import io.heapy.komok.User
import io.heapy.komok.UserContext
import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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

    override fun Route.install() {
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
