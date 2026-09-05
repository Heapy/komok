package io.heapy.komok.tech.runtime

/**
 * Thrown at runtime when a binding re-enters itself within the same logical
 * [KomokContext] (the [path] ends with the offending repeated [BindingId]).
 *
 * A read from a different coroutine/context is a wait, not a cycle; only a reentrant
 * read in the same caller-stack is reported here.
 */
class KomokCycleException(
    val path: List<BindingId>,
) : RuntimeException(
    "Binding cycle detected: ${path.joinToString(" -> ") { it.display() }}",
)
