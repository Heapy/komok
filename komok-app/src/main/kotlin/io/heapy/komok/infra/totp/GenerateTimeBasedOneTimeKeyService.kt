package io.heapy.komok.infra.totp

import io.heapy.komok.infra.base32.Base32
import java.security.SecureRandom

class GenerateTimeBasedOneTimeKeyService(
    private val random: SecureRandom,
    private val base32: Base32,
) {
    enum class Algorithm(
        val keyLength: Int,
    ) {
        SHA1(20),
        SHA256(32),
        SHA512(64),
    }

    fun generate(
        algorithm: Algorithm,
    ): String {
        val bytes = ByteArray(algorithm.keyLength)
        random.nextBytes(bytes)
        return base32.encode(bytes)
    }
}
