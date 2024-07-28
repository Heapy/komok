package io.heapy.komok

interface UserContext {
    val user: User
}

fun UserContext(
    user: User,
): UserContext =
    DefaultUserContext(
        user = user,
    )

private data class DefaultUserContext(
    override val user: User,
) : UserContext

interface User {
    val id: String
}

/**
 * Create a new User instance.
 *
 * @param id The user's unique identifier.
 */
fun User(
    id: String,
): User =
    DefaultUser(
        id = id,
    )

private data class DefaultUser(
    override val id: String,
) : User
