package io.heapy.komok.infra.password

import java.security.SecureRandom

class PasswordGenerator(
    private val random: SecureRandom,
) {
    fun generate(): String {
        val length = (MIN_LENGTH..MAX_LENGTH).random()
        return buildString(length) {
            repeat(length) {
                val index = random.nextInt(CHARS.length)
                append(CHARS[index])
            }
        }
    }

    private companion object {
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-=_+`~,.<>/?;:'[]{}"
        private const val MIN_LENGTH = 64
        private const val MAX_LENGTH = 256
    }
}
