package io.heapy.komok.tech.runtime

/**
 * Signature part of a [BindingId]: provider [name] plus a descriptor of its value
 * parameters.
 *
 * Return type is deliberately **not** part of identity — widening a provider's result
 * type (e.g. `HikariDataSource` -> `DataSource`) must not silently change the address
 * and break fork addressing or a persisted `/graph`. In v1 public providers have no
 * value parameters, so [valueParamsDescriptor] is empty; the slot is reserved so a
 * future overload/inherited provider does not break the format.
 */
data class ProviderSignature(
    val name: String,
    val valueParamsDescriptor: String = "",
)

/**
 * Stable address of a binding for memoization, fork replacement and introspection.
 *
 * Identity is `(moduleFqn, signature)` within one [KomokRuntime] — canonical module
 * type, not constructor path. A module type referenced from several constructor paths
 * resolves to one [BindingId] per provider.
 */
data class BindingId(
    val moduleFqn: String,
    val signature: ProviderSignature,
) {
    fun display(): String =
        "$moduleFqn.${signature.name}"
}
