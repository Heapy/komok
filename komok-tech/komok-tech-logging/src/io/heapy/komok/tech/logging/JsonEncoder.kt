package io.heapy.komok.tech.logging

/**
 * JSON encoder — structured logging output.
 *
 * All MDC entries become top-level fields in the JSON object.
 * Throwable becomes a `stacktrace` field if present.
 *
 * Uses manual JSON building to avoid external dependencies.
 */
class JsonEncoder(
    private val prettyPrint: Boolean = false,
    private val staticFields: Map<String, String> = emptyMap(),
) : Encoder {

    override fun encode(event: LogEvent): String {
        val fields = linkedMapOf<String, String>()

        fields["timestamp"] = event.timestamp.toString()
        fields["level"] = event.level.name
        fields["logger"] = event.loggerName
        fields["thread"] = event.threadName
        fields["message"] = event.message

        event.mdc.entries().forEach { [k, v] -> fields[k] = v.toString() }

        fields.putAll(staticFields)

        event.throwable?.let { fields["stacktrace"] = it.toString() }

        return if (prettyPrint) {
            fields.entries.joinToString(
                separator = ",\n  ",
                prefix = "{\n  ",
                postfix = "\n}",
            ) { [k, v] -> "\"${escapeJson(k)}\": \"${escapeJson(v)}\"" }
        } else {
            fields.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) { [k, v] -> "\"${escapeJson(k)}\":\"${escapeJson(v)}\"" }
        }
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
