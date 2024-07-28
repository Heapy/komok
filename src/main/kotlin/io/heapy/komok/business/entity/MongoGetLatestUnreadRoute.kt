package io.heapy.komok.business.entity

import io.heapy.komok.User
import io.heapy.komok.UserContext
import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

class MongoGetLatestUnreadRoute(
    private val mongoEntityDao: MongoEntityDao,
) : KomokRoute {
    @Serializable
    data class Response(
        val entities: List<String>,
    )

    override fun Route.install() {
        get("/mongo/entity") {
            val req = call.parameters["page"]?.toIntOrNull() ?: 0

            val entities = with(UserContext(User(id = "1"))) {
                mongoEntityDao.getLatest(
                    limit = 100,
                    offset = 100 * req,
                )
            }

            call.respond(Response(entities = entities))
        }
    }
}
