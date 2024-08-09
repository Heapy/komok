package io.heapy.komok.dao.pg

import io.heapy.komok.auth.common.User
import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jooq.DSLContext

class EntityInsertRoute(
    private val entityDao: EntityDao,
    private val dslContext: DSLContext,
) : KomokRoute {
    @Serializable
    data class Response(
        val id: Long,
    )

    @Serializable
    data class Request(
        val title: String,
        val text: String,
        val date: String,
        val readStatus: Boolean,
    )

    override fun Routing.install() {
        post("/entity") {
            val req = call.receive<Request>()

            val id = with(UserContext(User(id = "1"))) {
                with(TransactionContext(dslContext)) {
                    entityDao.insertPost(
                        title = req.title,
                        text = req.text,
                        date = req.date,
                        readStatus = req.readStatus,
                    )
                }
            }

            call.respond(Response(id = id!!))
        }
    }
}
