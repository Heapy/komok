package io.heapy.komok.infra.http.server.errors

class AuthorizationException(
    val response: String,
) : RuntimeException()

@Suppress("NOTHING_TO_INLINE")
inline fun authorizationError(
    message: String,
): Nothing {
    throw AuthorizationException(
        response = message,
    )
}
