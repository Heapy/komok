package io.heapy.komok

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class TestTimeSource(
    private var advanceBy: Duration = 1.seconds,
    var initial: Instant = Instant.now(),
) : TimeSource {
    private var current: Instant = initial
    private var _calls: Int = 0

    private fun getTimeContextAndAdvance(): TimeSource {
        _calls += 1
        val instant = current
        current = current.plus(advanceBy.toJavaDuration())
        val clock = Clock.fixed(
            instant,
            ZoneId.systemDefault(),
        )
        return TimeContext(clock)
    }

    val calls: Int
        get() = _calls

    fun reset(
        new: Instant? = null,
    ) {
        _calls = 0
        current = new
            ?: initial
    }

    override val clock: Clock
        get() = getTimeContextAndAdvance().clock

    override fun instant(): Instant =
        getTimeContextAndAdvance().instant()

    override fun localDateTime(): LocalDateTime =
        getTimeContextAndAdvance().localDateTime()

    override fun localDate(): LocalDate =
        getTimeContextAndAdvance().localDate()

    override fun localTime(): LocalTime =
        getTimeContextAndAdvance().localTime()

    override fun zonedDateTime(): ZonedDateTime =
        getTimeContextAndAdvance().zonedDateTime()

    override fun offsetDateTime(): OffsetDateTime =
        getTimeContextAndAdvance().offsetDateTime()

    override fun offsetTime(): OffsetTime =
        getTimeContextAndAdvance().offsetTime()
}
