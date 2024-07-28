@file:OptIn(KtorExperimentalLocationsAPI::class)

package io.heapy.komok.business.user

import io.heapy.komok.infra.server.KomokRoute
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRoute : KomokRoute {
    @Location("/person/{name}")
    class PersonRequest(val name: String)

    override fun Route.install() {
        get<PersonRequest> {
            val person = withContext(Dispatchers.IO) {
                it.name
            }
            call.respond(person)
        }
    }
}
