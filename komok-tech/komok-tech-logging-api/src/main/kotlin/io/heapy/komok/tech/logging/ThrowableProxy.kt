package io.heapy.komok.tech.logging

import java.util.IdentityHashMap

/**
 * Proxy for a stack trace element with pre-computed string representation.
 */
class StackTraceElementProxy(
    val stackTraceElement: StackTraceElement,
) {
    val string: String by lazy { stackTraceElement.toString() }

    override fun toString(): String = string
}

/**
 * Immutable snapshot of a [Throwable] with cycle detection and common-frame elision.
 *
 * Captures the full exception chain (cause + suppressed) at construction time,
 * using an [IdentityHashMap] to detect cycles in the cause chain.
 */
class ThrowableProxy(
    val className: String,
    val message: String?,
    val stackTrace: Array<StackTraceElementProxy>,
    val cause: ThrowableProxy?,
    val suppressed: Array<ThrowableProxy>,
    val commonFramesCount: Int,
) {
    override fun toString(): String = buildString {
        appendProxy(this@ThrowableProxy, "")
    }

    companion object {
        fun create(throwable: Throwable): ThrowableProxy {
            return create(throwable, IdentityHashMap())
        }

        private fun create(
            throwable: Throwable,
            visited: IdentityHashMap<Throwable, Boolean>,
        ): ThrowableProxy {
            visited[throwable] = true

            val stackTrace = throwable.stackTrace.map { StackTraceElementProxy(it) }.toTypedArray()

            val causeProxy = throwable.cause?.let { cause ->
                if (visited.containsKey(cause)) {
                    ThrowableProxy(
                        className = cause.javaClass.name,
                        message = "[CIRCULAR REFERENCE: ${cause.message}]",
                        stackTrace = emptyArray(),
                        cause = null,
                        suppressed = emptyArray(),
                        commonFramesCount = 0,
                    )
                } else {
                    val proxy = create(cause, visited)
                    val commonFrames = countCommonFrames(stackTrace, proxy.stackTrace)
                    ThrowableProxy(
                        className = proxy.className,
                        message = proxy.message,
                        stackTrace = proxy.stackTrace,
                        cause = proxy.cause,
                        suppressed = proxy.suppressed,
                        commonFramesCount = commonFrames,
                    )
                }
            }

            val suppressedProxies = throwable.suppressedExceptions.map { suppressed ->
                if (visited.containsKey(suppressed)) {
                    ThrowableProxy(
                        className = suppressed.javaClass.name,
                        message = "[CIRCULAR REFERENCE: ${suppressed.message}]",
                        stackTrace = emptyArray(),
                        cause = null,
                        suppressed = emptyArray(),
                        commonFramesCount = 0,
                    )
                } else {
                    create(suppressed, visited)
                }
            }.toTypedArray()

            return ThrowableProxy(
                className = throwable.javaClass.name,
                message = throwable.message,
                stackTrace = stackTrace,
                cause = causeProxy,
                suppressed = suppressedProxies,
                commonFramesCount = 0,
            )
        }

        private fun countCommonFrames(
            enclosing: Array<StackTraceElementProxy>,
            enclosed: Array<StackTraceElementProxy>,
        ): Int {
            var ei = enclosing.size - 1
            var di = enclosed.size - 1
            var count = 0
            while (ei >= 0 && di >= 0) {
                if (enclosing[ei].stackTraceElement == enclosed[di].stackTraceElement) {
                    count++
                    ei--
                    di--
                } else {
                    break
                }
            }
            return count
        }
    }
}

private fun StringBuilder.appendProxy(proxy: ThrowableProxy, prefix: String) {
    append(prefix)
    append(proxy.className)
    if (proxy.message != null) {
        append(": ")
        append(proxy.message)
    }
    append('\n')

    val framesToShow = proxy.stackTrace.size - proxy.commonFramesCount
    for (i in 0 until framesToShow) {
        append(prefix)
        append("\tat ")
        append(proxy.stackTrace[i])
        append('\n')
    }
    if (proxy.commonFramesCount > 0) {
        append(prefix)
        append("\t... ")
        append(proxy.commonFramesCount)
        append(" common frames omitted\n")
    }

    for (suppressed in proxy.suppressed) {
        appendProxy(suppressed, "${prefix}\tSuppressed: ")
    }

    proxy.cause?.let { cause ->
        append(prefix)
        append("Caused by: ")
        appendProxy(cause, prefix)
    }
}
