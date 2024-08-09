package io.heapy.komok.dao.pg

import io.heapy.komok.auth.common.User
import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jooq.DSLContext

class UpdateStatusRoute(
    private val entityDao: EntityDao,
    private val dslContext: DSLContext,
) : KomokRoute {
    @Serializable
    data class EntityRequest(
        val id: Long,
    )

    @Serializable
    data class Response(
        val ok: Boolean,
    )

    override fun Routing.install() {
        put("/entity") {
            val req = call.receive<EntityRequest>()

            val resp = with(UserContext(User(id = "1"))) {
                with(TransactionContext(dslContext)) {
                    entityDao.markPostAsRead(
                        req.id
                    )
                }
            }

            call.respond(Response(ok = resp == 1))
        }
    }
}
