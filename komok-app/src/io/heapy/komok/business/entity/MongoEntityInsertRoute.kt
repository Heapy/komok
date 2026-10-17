package io.heapy.komok.business.entity

import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

class MongoEntityInsertRoute(
    private val mongoEntityDao: MongoEntityDao,
) : KomokRoute {
    override fun Routing.install() {
        post("/entity") {
            withUser {
                val req = call.receive<Request>()

                val id = mongoEntityDao.insertPost(
                    title = req.title,
                    text = req.text,
                    date = req.date,
                    readStatus = req.readStatus,
                )

                call.respond(Response(id = id!!))
            }
        }
    }

    @Serializable
    data class Request(
        val title: String,
        val text: String,
        val date: String,
        val readStatus: Boolean,
    )

    @Serializable
    data class Response(
        val id: String,
    )
}
