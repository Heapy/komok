package io.heapy.komok.business.entity

import io.heapy.komok.User
import io.heapy.komok.UserContext
import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable

class MongoUpdateStatusRoute(
    private val mongoEntityDao: MongoEntityDao,
) : KomokRoute {
    @Serializable
    data class EntityRequest(
        val id: String,
    )

    @Serializable
    data class Response(
        val ok: Boolean,
    )

    override fun Route.install() {
        put("/mongo/entity") {
            val req = call.receive<EntityRequest>()

            val resp = with(UserContext(User(id = "1"))) {
                mongoEntityDao.markPostAsRead(
                    req.id
                )
            }

            call.respond(Response(ok = resp == 1L))
        }
    }
}
