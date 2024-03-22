package io.heapy.komok.sync

import java.time.Clock
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

sealed class HLCDecodeError : Exception() {
    data class TimestampDecodeFailure(
        val encodedClock: String,
    ) : HLCDecodeError()

    data class CounterDecodeFailure(
        val encodedClock: String,
    ) : HLCDecodeError()

    data class NodeDecodeFailure(
        val encodedClock: String,
    ) : HLCDecodeError()
}

sealed class HLCError : Exception() {
    data class DuplicateNodeError(
        val nodeID: NodeID,
    ) : HLCError()

    data class ClockDriftError(
        val local: Instant,
        val now: Instant,
    ) : HLCError()

    data object CausalityOverflowError : HLCError() {
        private fun readResolve(): Any =
            CausalityOverflowError
    }
}

class HLCComparator : Comparator<HybridLogicalClock> {
    override fun compare(
        first: HybridLogicalClock,
        second: HybridLogicalClock,
    ): Int {
        return if (first.timestamp.epochMillis == second.timestamp.epochMillis) {
            if (first.counter == second.counter) {
                first.node.identifier.compareTo(second.node.identifier)
            } else {
                first.counter - second.counter
            }
        } else {
            first.timestamp.epochMillis.compareTo(second.timestamp.epochMillis)
        }
    }
}

fun main() {
    val instant = Instant.now(Clock.systemUTC())

    val c0 = HybridLogicalClock(
        timestamp = instant,
        node = NodeID.mint(),
        counter = 0,
    )

    val c1 = HybridLogicalClock(
        timestamp = instant,
        node = NodeID.mint(),
        counter = 0,
    )

    val c2 = HybridLogicalClock.localTick(
        c1,
        Instant.now(Clock.systemUTC()),
    )

    println(
        """
        c0: $c0
        c1: $c1
        c2: $c2
    """.trimIndent(),
    )

    println(
        HLCComparator().compare(
            c0,
            c1,
        ),
    )
    println(
        HLCComparator().compare(
            c1,
            c2,
        ),
    )
    println(
        HLCComparator().compare(
            c1,
            c1,
        ),
    )
    println(
        HLCComparator().compare(
            c2,
            c1,
        ),
    )
}

data class HybridLogicalClock(
    val timestamp: Instant,
    val node: NodeID,
    val counter: Int,
) {

    companion object {
        /**
         * This should be called every time a new event is generated locally, the result becomes the events timestamp and the new local time
         */
        fun localTick(
            local: HybridLogicalClock,
            wallClockTime: Instant,
            maxClockDrift: Int = 1000 * 60,
        ): HybridLogicalClock {
            return if (wallClockTime.epochMillis > local.timestamp.epochMillis) {
                local.copy(timestamp = wallClockTime)
            } else {
                val clock = local.copy(counter = local.counter + 1)
                validate(
                    clock,
                    wallClockTime,
                    maxClockDrift,
                )
            }
        }

        /**
         * This should be called every time a new event is received from a remote node, the result becomes the new local time
         */
        fun remoteTock(
            local: HybridLogicalClock,
            remote: HybridLogicalClock,
            wallClockTime: Instant = Instant.now(Clock.systemUTC()),
            maxClockDrift: Int = 1000 * 60,
        ): HybridLogicalClock {
            val clock = when {
                local.node.identifier == remote.node.identifier -> {
                    throw HLCError.DuplicateNodeError(local.node)
                }

                wallClockTime.epochMillis > local.timestamp.epochMillis &&
                      wallClockTime.epochMillis > remote.timestamp.epochMillis -> {
                    local.copy(
                        timestamp = wallClockTime,
                        counter = 0,
                    )
                }

                local.timestamp.epochMillis == remote.timestamp.epochMillis -> {
                    local.copy(
                        counter = max(
                            local.counter,
                            remote.counter,
                        ) + 1,
                    )
                }

                local.timestamp.epochMillis > remote.timestamp.epochMillis -> {
                    local.copy(counter = local.counter + 1)
                }

                else -> local.copy(
                    timestamp = remote.timestamp,
                    counter = remote.counter + 1,
                )
            }

            return validate(
                clock,
                wallClockTime,
                maxClockDrift,
            )
        }

        private fun validate(
            clock: HybridLogicalClock,
            now: Instant,
            maxClockDrift: Int,
        ): HybridLogicalClock {
            if (clock.counter > 36f
                    .pow(5)
                    .toInt()
            ) {
                throw HLCError.CausalityOverflowError
            }

            if (abs(clock.timestamp.epochMillis - now.epochMillis) > maxClockDrift) {
                throw HLCError.ClockDriftError(
                    clock.timestamp,
                    now,
                )
            }

            return clock
        }

        fun encodeToString(hlc: HybridLogicalClock): String {
            return with(hlc) {
                "${
                    timestamp.epochMillis
                        .toString()
                        .padStart(
                            15,
                            '0',
                        )
                }:${
                    counter
                        .toString(36)
                        .padStart(
                            5,
                            '0',
                        )
                }:${node.identifier}"
            }
        }

        fun decodeFromString(encoded: String): HybridLogicalClock {
            val parts = encoded.split(":")

            if (parts.size < 3) throw HLCDecodeError.TimestampDecodeFailure(encoded)

            val timestamp = parts
                .firstOrNull()
                ?.let {
                    Instant(it.toLong())
                }
                ?: throw HLCDecodeError.TimestampDecodeFailure(encoded)

            val counter = parts
                .getOrNull(1)
                ?.toInt(36)
                ?: throw HLCDecodeError.CounterDecodeFailure(encoded)

            val node = parts
                .getOrNull(2)
                ?.let { NodeID(it) }
                ?: throw HLCDecodeError.NodeDecodeFailure(encoded)

            return HybridLogicalClock(
                timestamp = timestamp,
                node = node,
                counter = counter,
            )
        }
    }

    override fun toString(): String {
        return encodeToString(this)
    }
}

@JvmInline
value class NodeID(
    val identifier: String,
) {
    companion object {
        fun mint(
            uuid: UUID = UUID.randomUUID(),
        ) =
            NodeID(
                uuid
                    .toString()
                    .replace(
                        "-",
                        "",
                    )
                    .takeLast(16),
            )
    }
}

@JvmInline
value class Instant(
    val epochMillis: Long,
) {
    companion object {
        fun now(clock: Clock) =
            Instant(clock.millis())
    }
}

