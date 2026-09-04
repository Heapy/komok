package io.heapy.komok.tech.di.delegate

/**
 * Identity of the current thread, compared with `==`.
 *
 * Targets without threads return one shared token.
 */
internal expect fun currentThreadToken(): Any
