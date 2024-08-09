package io.heapy.komok.business.user

import io.heapy.komok.server.common.KomokRoute
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRoute : KomokRoute {
    @Resource("/person/{name}")
    class PersonRequest(val name: String)

    override fun Routing.install() {
        get<PersonRequest> {
            val person = withContext(Dispatchers.IO) {
                it.name
            }
            call.respond(person)
        }
    }
}
