package io.heapy.komok.business.user.session

import java.security.SecureRandom

interface SessionTokenGenerator {
    fun generate(): String
}

class SecureRandomSessionTokenGenerator(
    private val random: SecureRandom,
) : SessionTokenGenerator {
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
