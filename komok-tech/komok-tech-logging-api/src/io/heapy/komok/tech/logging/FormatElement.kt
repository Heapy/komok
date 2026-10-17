package io.heapy.komok.tech.logging

/**
 * Format element for programmatic format building.
 * No pattern string parsing — type-safe, IDE-friendly.
 */
sealed class FormatElement {
    data object Timestamp : FormatElement()
    data object Message : FormatElement()
    data object Level : FormatElement()
    data object LoggerName : FormatElement()
    data class LoggerNameShort(val segments: Int = 1) : FormatElement()
    data object ThreadName : FormatElement()
    data object AllMdc : FormatElement()
    data class MdcValue(val key: String) : FormatElement()
    data class Literal(val text: String) : FormatElement()
    data object Newline : FormatElement()
    data object ThrowableTrace : FormatElement()
}
