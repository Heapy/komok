package io.heapy.komok.business.entity

import io.heapy.komok.auth.common.UserContext
import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

class MongoGetLatestUnreadRoute(
    private val mongoEntityDao: MongoEntityDao,
) : KomokRoute {
    @Serializable
    data class Response(
        val entities: List<String>,
    )

    override fun Routing.install() {
        get("/mongo/entity") {
            val req = call.parameters["page"]?.toIntOrNull() ?: 0

            val entities = with(UserContext(id = "1")) {
                mongoEntityDao.getLatest(
                    limit = 100,
                    offset = 100 * req,
                )
            }

            call.respond(Response(entities = entities))
        }
    }
}
