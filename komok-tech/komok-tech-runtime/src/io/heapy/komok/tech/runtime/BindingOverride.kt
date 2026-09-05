package io.heapy.komok.tech.runtime

/**
 * A fork replacement for one binding.
 *
 * [Value] pins a ready instance (often collapsing the cone to a leaf). [Provider] holds
 * a monomorphic thunk — the user-written receiver lambda has already captured its scope
 * in generated code (variant B), so the runtime just calls `thunk(context)`; the thunk
 * runs lazily on first read in the child realm and records edges like any provider.
 */
sealed interface BindingOverride<out T> {
    data class Value<out T>(val value: T) : BindingOverride<T>

    class Provider<out T>(
        val thunk: suspend context(KomokContext) () -> T,
    ) : BindingOverride<T>
}
