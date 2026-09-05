package io.heapy.komok.tech.runtime

/**
 * Serializable `/graph` payload: the static module graph (structural truth, full at
 * compile time) overlaid with the binding [edges] actually recorded at runtime. No
 * reflection — generated code supplies [modules] and the runtime supplies [edges].
 */
data class GraphPayload(
    val modules: List<ModuleDescriptor>,
    val edges: List<EdgeDescriptor>,
) {
    /** Deterministic JSON (sorted, escaped), built without reflection or a serialization library. */
    fun toJson(): String = buildString {
        append("{\"modules\":[")
        modules.sortedBy { it.fqn }.forEachIndexed { index, module ->
            if (index > 0) append(",")
            append("{\"fqn\":")
            appendQuoted(module.fqn)
            append(",\"dependencies\":")
            appendStringArray(module.dependencies.sorted())
            append(",\"bindings\":")
            appendStringArray(module.bindings.sorted())
            append("}")
        }
        append("],\"edges\":[")
        edges.sortedWith(compareBy({ it.from }, { it.to })).forEachIndexed { index, edge ->
            if (index > 0) append(",")
            append("{\"from\":")
            appendQuoted(edge.from)
            append(",\"to\":")
            appendQuoted(edge.to)
            append("}")
        }
        append("]}")
    }
}

/** One module node: its [fqn], the module types it [dependencies] on, and its [bindings]. */
data class ModuleDescriptor(
    val fqn: String,
    val dependencies: List<String>,
    val bindings: List<String>,
)

/** One recorded binding edge `from -> to` (display addresses). */
data class EdgeDescriptor(
    val from: String,
    val to: String,
)

/** Binding edges (a fact of execution) as display-string descriptors. */
fun KomokRuntime.edgeDescriptors(): List<EdgeDescriptor> =
    edges().map { EdgeDescriptor(it.from.display(), it.to.display()) }

private fun StringBuilder.appendStringArray(values: List<String>) {
    append("[")
    values.forEachIndexed { index, value ->
        if (index > 0) append(",")
        appendQuoted(value)
    }
    append("]")
}

// Backtick-quoted Kotlin identifiers may contain quotes and other metacharacters, so
// names cannot be assumed JSON-safe.
private fun StringBuilder.appendQuoted(value: String) {
    append('"')
    value.forEach { ch ->
        when {
            ch == '"' -> append("\\\"")
            ch == '\\' -> append("\\\\")
            ch == '\n' -> append("\\n")
            ch == '\r' -> append("\\r")
            ch == '\t' -> append("\\t")
            ch < ' ' -> append("\\u").append(ch.code.toString(16).padStart(4, '0'))
            else -> append(ch)
        }
    }
    append('"')
}
