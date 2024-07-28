package io.heapy.komok.business.entity

import io.heapy.komok.TransactionContext
import io.heapy.komok.User
import io.heapy.komok.UserContext
import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
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

    override fun Route.install() {
        post("/entity") {
            this.withUser {

            }
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

private fun PipelineContext<*, ApplicationCall>.withUser(
    function: UserContext.() -> Unit,
) {
    val principal = call.principal<JWTPrincipal>()
    principal ?: error("No principal found")
    val user = User(principal.payload.getClaim("id").asString())

    val username = principal.payload
        .getClaim("username")
        .asString()
    val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
}
