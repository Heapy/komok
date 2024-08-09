package io.heapy.komok.business.entity

import io.heapy.komok.auth.common.User
import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
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

    override fun Routing.install() {
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
