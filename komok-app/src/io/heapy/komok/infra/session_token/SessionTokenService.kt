package io.heapy.komok.infra.session_token

import java.security.SecureRandom

interface SessionTokenService {
    fun generate(): String
}

class SecureRandomSessionTokenService(
    private val random: SecureRandom,
) : SessionTokenService {
    override fun generate(): String {
        return buildString(SESSION_TOKEN_LENGTH) {
            repeat(SESSION_TOKEN_LENGTH) {
                val index = random.nextInt(CHARS.length)
                append(CHARS[index])
            }
        }
    }

    private companion object {
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        const val SESSION_TOKEN_LENGTH = 128
    }
}
