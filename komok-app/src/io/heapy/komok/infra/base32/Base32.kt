package io.heapy.komok.infra.base32

import java.nio.ByteBuffer
import java.util.*

class Base32 {
    fun decode(
        base32: String,
    ): ByteArray {
        val data = base32
            .uppercase(Locale.ROOT)
            .replace(
                "=",
                ""
            )
            .toCharArray()
        val buffer = ByteBuffer.allocate(data.size * 5 / 8)

        var bits = 0
        var value = 0

        data.forEach { char ->
            val index = CHAR_MAP.indexOf(char)
            if (index == -1) throw IllegalArgumentException("Invalid character in Base32 string: $base32")

            value = (value shl 5) or index
            bits += 5

            if (bits >= 8) {
                bits -= 8
                buffer.put((value shr bits).toByte())
            }
        }

        return buffer.array()
    }

    fun encode(bytes: ByteArray): String {
        val result = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        bytes.forEach { byte ->
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8

            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                result.append(CHAR_MAP[index])
                bitsLeft -= 5
            }
        }

        if (bitsLeft > 0) {
            buffer = buffer shl (5 - bitsLeft)
            val index = buffer and 0x1F
            result.append(CHAR_MAP[index])
        }

        while (result.length % 8 != 0) {
            result.append('=')
        }

        return result.toString()
    }

    private companion object {
        private val CHAR_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()
    }
}
