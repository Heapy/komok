package io.heapy.komok.tech.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime

interface TimeSource {
    val clock: Clock
    fun instant(): Instant
    fun localDateTime(): LocalDateTime
    fun localDate(): LocalDate
    fun localTime(): LocalTime
    fun zonedDateTime(): ZonedDateTime
    fun offsetDateTime(): OffsetDateTime
    fun offsetTime(): OffsetTime
}

fun TimeSource(
    clock: Clock,
): TimeSource =
    DefaultTimeSource(
        clock = clock,
    )

private data class DefaultTimeSource(
    override val clock: Clock,
) : TimeSource {
    override fun instant(): Instant =
        clock.instant()

    override fun localDateTime(): LocalDateTime =
        LocalDateTime.now(clock)

    override fun localDate(): LocalDate =
        LocalDate.now(clock)

    override fun localTime(): LocalTime =
        LocalTime.now(clock)

    override fun zonedDateTime(): ZonedDateTime =
        ZonedDateTime.now(clock)

    override fun offsetDateTime(): OffsetDateTime =
        OffsetDateTime.now(clock)

    override fun offsetTime(): OffsetTime =
        OffsetTime.now(clock)
}
