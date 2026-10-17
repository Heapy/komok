package io.heapy.komok.tech.logging

import io.heapy.komok.tech.logging.FormatElement.AllMdc
import io.heapy.komok.tech.logging.FormatElement.Level
import io.heapy.komok.tech.logging.FormatElement.Literal
import io.heapy.komok.tech.logging.FormatElement.LoggerName
import io.heapy.komok.tech.logging.FormatElement.LoggerNameShort
import io.heapy.komok.tech.logging.FormatElement.MdcValue
import io.heapy.komok.tech.logging.FormatElement.Message
import io.heapy.komok.tech.logging.FormatElement.Newline
import io.heapy.komok.tech.logging.FormatElement.ThreadName
import io.heapy.komok.tech.logging.FormatElement.ThrowableTrace
import io.heapy.komok.tech.logging.FormatElement.Timestamp

/**
 * Encoder that renders [LogEvent] according to a list of [FormatElement]s.
 * No pattern string parsing — format is built programmatically via DSL.
 */
class FormatEncoder(
    private val elements: List<FormatElement>,
    private val mdcSeparator: String = ", ",
) : Encoder {

    override fun encode(event: LogEvent): String = buildString(128) {
        for (element in elements) {
            when (element) {
                is Timestamp -> append(event.timestamp)
                is Message -> append(event.message)
                is Level -> append(event.level.name.padEnd(5))
                is LoggerName -> append(event.loggerName)
                is LoggerNameShort -> {
                    val parts = event.loggerName.split('.')
                    append(parts.takeLast(element.segments).joinToString("."))
                }
                is ThreadName -> append(event.threadName)
                is AllMdc -> {
                    append(
                        event.mdc.entries().entries
                            .joinToString(mdcSeparator) { [k, v] -> "$k=$v" },
                    )
                }
                is MdcValue -> {
                    append(event.mdc.entries()[element.key] ?: "")
                }
                is Literal -> append(element.text)
                is Newline -> append('\n')
                is ThrowableTrace -> {
                    event.throwable?.let { proxy ->
                        append('\n')
                        append(proxy.toString())
                    }
                }
            }
        }
    }
}

/**
 * DSL builder for constructing a [FormatEncoder].
 */
@LoggingDslMarker
class FormatBuilder {
    private val elements = mutableListOf<FormatElement>()

    fun timestamp() { elements += Timestamp
    }
    fun message() { elements += Message
    }
    fun level() { elements += Level
    }
    fun loggerName() { elements += LoggerName
    }
    fun loggerShort(segments: Int = 1) { elements += LoggerNameShort(segments)
    }
    fun threadName() { elements += ThreadName
    }
    fun allMdc() { elements += AllMdc
    }
    fun mdc(key: String) { elements += MdcValue(key)
    }
    fun literal(text: String) { elements += Literal(text)
    }
    fun newline() { elements += Newline
    }
    fun throwableTrace() { elements += ThrowableTrace
    }

    fun build(): FormatEncoder = FormatEncoder(elements.toList())
}

fun format(block: FormatBuilder.() -> Unit): FormatEncoder =
    FormatBuilder().apply(block).build()
