package io.heapy.komok.tech.runtime.test.wrapper

/** Function-only dependency type used by runtime-test to verify explicit mock generation. */
fun interface ExternalFormatter {
    fun format(value: String): String
}
