package io.heapy.komok.tech.logging

/**
 * Transforms a [LogEvent] into a string representation.
 * Used by appenders to format output.
 */
fun interface Encoder {
    fun encode(event: LogEvent): String
}
