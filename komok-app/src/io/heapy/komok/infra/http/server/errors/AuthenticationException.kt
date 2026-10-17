package io.heapy.komok.infra.http.server.errors

class AuthenticationException(
    val response: AuthenticationError,
) : RuntimeException()

@Suppress("NOTHING_TO_INLINE")
inline fun authenticationError(
    type: AuthenticationError,
): Nothing {
    throw AuthenticationException(
        response = type,
    )
}

enum class AuthenticationError {
    INVALID_SESSION,
    SESSION_EXPIRED,
}
