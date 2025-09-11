package io.heapy.komok.auth.common

interface UserContext {
    val id: String
}

/**
 * Create a new UserContext instance.
 *
 * @param id The user's unique identifier.
 */
fun UserContext(
    id: String,
): UserContext =
    DefaultUserContext(
        id = id,
    )

private data class DefaultUserContext(
    override val id: String,
) : UserContext
