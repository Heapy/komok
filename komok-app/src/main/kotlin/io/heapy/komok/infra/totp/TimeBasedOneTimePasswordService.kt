package io.heapy.komok.infra.totp

import io.heapy.komok.infra.time.TimeSource
import io.heapy.komok.infra.base32.Base32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

class TimeBasedOneTimePasswordService(
    private val base32: Base32,
    private val timeSource: TimeSource,
) {
    fun generate(
        secret: String,
    ): String {
        val key = base32.decode(secret)
        val timeWindow = timeSource.instant().epochSecond / TIME_STEP_SECONDS

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

        val otp = binary % 10.0.pow(TOTP_DIGITS.toDouble())
            .toInt()

        return otp
            .toString()
            .padStart(
                TOTP_DIGITS,
                '0'
            )
    }

    fun validate(
        secret: String,
        totp: String,
    ): Boolean {
        val calculatedOtp = generate(secret)

        return calculatedOtp == totp
    }

    private companion object {
        private const val HMAC_ALGO = "HmacSHA1"
        private const val TIME_STEP_SECONDS = 30L
        private const val TOTP_DIGITS = 6
    }
}
