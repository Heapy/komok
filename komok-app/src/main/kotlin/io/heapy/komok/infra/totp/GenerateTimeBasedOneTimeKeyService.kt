package io.heapy.komok.infra.totp

import io.heapy.komok.infra.base32.Base32
import java.security.SecureRandom

class GenerateTimeBasedOneTimeKeyService(
    private val random: SecureRandom,
    private val base32: Base32,
) {
    fun generate(): String {
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return base32.encode(bytes)
    }
}
