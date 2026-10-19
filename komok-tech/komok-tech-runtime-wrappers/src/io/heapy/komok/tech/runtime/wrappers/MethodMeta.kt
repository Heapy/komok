package io.heapy.komok.tech.runtime.wrappers

/**
 * Static, generated metadata for one wrapped service method — the reflection-free
 * replacement for `KCallable`/`KFunction`. The compiler emits one static [MethodMeta] per
 * wrapped method; wrapper helpers ([logCall], [metricCall], [retryCall]) read it instead
 * of looking anything up at runtime.
 */
data class MethodMeta(
    val owner: String,
    val name: String,
    val parameters: List<ParameterMeta>,
    val returnType: String,
    val isSuspend: Boolean,
)

/** A single wrapped-method parameter: its [name] and declared [type] FQN. */
data class ParameterMeta(
    val name: String,
    val type: String,
)
