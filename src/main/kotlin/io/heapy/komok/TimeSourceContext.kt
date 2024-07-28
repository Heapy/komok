package io.heapy.komok

interface TimeSourceContext {
    val timeSource: TimeSource
}

fun TimeSourceContext(
    timeSource: TimeSource,
): TimeSourceContext =
    DefaultTimeSourceContext(
        timeSource = timeSource,
    )

@JvmInline
private value class DefaultTimeSourceContext(
    override val timeSource: TimeSource,
) : TimeSourceContext
