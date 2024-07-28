package io.heapy.komok.business.entity

import io.heapy.komok.User
import io.heapy.komok.UserContext
import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

class MongoEntityInsertRoute(
    private val mongoEntityDao: MongoEntityDao,
) : KomokRoute {
    @Serializable
    data class Response(
        val id: String,
    )

    @Serializable
    data class Request(
        val title: String,
        val text: String,
        val date: String,
        val readStatus: Boolean,
    )

    override fun Route.install() {
        post("/mongo/entity") {
            val req = call.receive<Request>()

            val id = with(UserContext(User(id = "1"))) {
                mongoEntityDao.insertPost(
                    title = req.title,
                    text = req.text,
                    date = req.date,
                    readStatus = req.readStatus,
                )
            }

            call.respond(Response(id = id!!))
        }
    }
}
