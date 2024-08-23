package io.heapy.komok.business.user.totp

import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TOTP {
    @JvmStatic
    fun main(args: Array<String>) {
        println(generateTOTP("YKSY2DKTFNNMDG6J"))
    }

    private const val HMAC_ALGO = "HmacSHA1"
    private const val TIME_STEP_SECONDS = 30L // TOTP step period in seconds
    private const val TOTP_DIGITS = 6

    fun generateTOTP(secret: String): String {
        val key = Base32.decode(secret)
        val timeWindow = currentTimeMillis() / 1000 / TIME_STEP_SECONDS

        val data = ByteBuffer
            .allocate(8)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(timeWindow)
            .array()

        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(
            SecretKeySpec(
                key,
                HMAC_ALGO
            )
        )

        val hmac = mac.doFinal(data)

        val offset = hmac[hmac.size - 1].toInt() and 0x0F
        val binary = (hmac[offset].toInt() and 0x7f shl 24) or
              (hmac[offset + 1].toInt() and 0xff shl 16) or
              (hmac[offset + 2].toInt() and 0xff shl 8) or
              (hmac[offset + 3].toInt() and 0xff)

        val otp = binary % Math
            .pow(
                10.0,
                TOTP_DIGITS.toDouble()
            )
            .toInt()

        return otp
            .toString()
            .padStart(
                TOTP_DIGITS,
                '0'
            )
    }

    fun validateTOTP(
        secret: String,
        otp: String,
        window: Int = 1
    ): Boolean {
        val key = Base32.decode(secret)

        val currentTimeWindow = currentTimeMillis() / 1000 / TIME_STEP_SECONDS

        for (i in -window..window) {
            val timeWindow = currentTimeWindow + i
            val data = ByteBuffer
                .allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(timeWindow)
                .array()

            val mac = Mac.getInstance(HMAC_ALGO)
            mac.init(
                SecretKeySpec(
                    key,
                    HMAC_ALGO
                )
            )

            val hmac = mac.doFinal(data)

            val offset = hmac[hmac.size - 1].toInt() and 0x0F
            val binary = (hmac[offset].toInt() and 0x7f shl 24) or
                  (hmac[offset + 1].toInt() and 0xff shl 16) or
                  (hmac[offset + 2].toInt() and 0xff shl 8) or
                  (hmac[offset + 3].toInt() and 0xff)

            val calculatedOtp = (binary % Math
                .pow(
                    10.0,
                    TOTP_DIGITS.toDouble()
                )
                .toInt())
                .toString()
                .padStart(
                    TOTP_DIGITS,
                    '0'
                )

            if (calculatedOtp == otp) {
                return true
            }
        }

        return false
    }
}

object Base32 {
    private val CHAR_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()

    fun decode(base32: String): ByteArray {
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
            if (index == -1) throw IllegalArgumentException("Invalid character in Base32 string")

            value = (value shl 5) or index
            bits += 5

            if (bits >= 8) {
                bits -= 8
                buffer.put((value shr bits).toByte())
            }
        }

        return buffer.array()
    }
}
