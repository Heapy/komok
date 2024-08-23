package io.heapy.komok.business.user.argon2

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.Argon2Parameters.Builder
import java.nio.charset.StandardCharsets
import java.util.random.RandomGenerator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface PasswordHasher {
    fun hash(password: String): String
    fun verify(
        password: String,
        hash: String,
    ): Boolean
}

fun main() {
    val hasher = createPasswordHasherModule {}
        .passwordHasher

    val hash = hasher.hash("password")

    println(hash)
}

@OptIn(ExperimentalEncodingApi::class)
class Argon2idPasswordHasher(
    private val json: Json,
    private val randomGenerator: RandomGenerator,
) : PasswordHasher {
    override fun hash(password: String): String {
        val salt = generateSalt16Byte()
        val hash = Base64.encode(
            generateArgon2idSensitive(
                password,
                salt,
            ),
        )

        return json.encodeToString(
            Hash.serializer(),
            Hash(
                hash = hash,
                salt = salt,
            ),
        )
    }

    internal fun generateArgon2idSensitive(
        password: String,
        salt: ByteArray,
    ): ByteArray {
        val opsLimit = 4
        val memLimit = 1048576
        val outputLength = 32
        val parallelism = 1
        val builder = Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13) // 19
            .withIterations(opsLimit)
            .withMemoryAsKB(memLimit)
            .withParallelism(parallelism)
            .withSalt(salt)
        val gen = Argon2BytesGenerator()
        gen.init(builder.build())
        val result = ByteArray(outputLength)
        gen.generateBytes(
            password.toByteArray(StandardCharsets.UTF_8),
            result,
            0,
            result.size,
        )
        return result
    }

    internal fun generateSalt16Byte(): ByteArray {
        val salt = ByteArray(16)
        randomGenerator.nextBytes(salt)
        return salt
    }

    override fun verify(
        password: String,
        hash: String,
    ): Boolean {
        val hashObj = json.decodeFromString(
            Hash.serializer(),
            hash,
        )
        val hashToVerify = generateArgon2idSensitive(
            password,
            hashObj.salt,
        )

        return Base64.encode(hashToVerify) == hashObj.hash
    }

    @Serializable
    private class Hash(
        val hash: String,
        val salt: ByteArray,
    )
}
