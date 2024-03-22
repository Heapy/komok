@file:OptIn(KtorExperimentalLocationsAPI::class)

package io.heapy.komok.business.routes

import io.heapy.komok.database.tables.daos.PersonDao
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext

@Location("/person/{name}")
class PersonRequest(val name: String)

fun Routing.person(dslContext: DSLContext) {
    get<PersonRequest> {
        val person = withContext(Dispatchers.IO) {
            PersonDao(dslContext.configuration()).fetchByName(it.name).single()
        }
        call.respond(person.toString())
    }
}
