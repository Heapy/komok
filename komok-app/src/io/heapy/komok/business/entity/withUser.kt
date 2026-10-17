package io.heapy.komok.business.entity

import io.heapy.komok.auth.common.UserContext
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.withUser(
    function: suspend UserContext.() -> Unit,
) {
    // Get the principal (JWT) from the call
    val principal = call.principal<JWTPrincipal>()

    // If no principal found, throw an error
    principal
        ?: error("No principal found")

    // Extract user ID from the JWT payload
    val userId = principal.payload
        .getClaim("id")
        .asString()

    // Calculate the expiration time (if it exists)
    val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())

    // Create UserContext
    val userContext = UserContext(
        id = userId,
    )

    // Invoke the passed function with the user context
    userContext.function()
}
