package io.heapy.komok.tech.runtime.test.wrapper

import io.heapy.komok.tech.runtime.wrappers.MethodMeta
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Test-only "third-party" wrapper: records every call of the annotated `suspend` method
 * into the caller-provided [AuditTrail] context parameter. Exists to prove that a new
 * wrapper kind ships as a plain library (annotation + runtime helper) without
 * changing the compiler plugin.
 */
@io.heapy.komok.tech.runtime.CallWrapper("io.heapy.komok.tech.runtime.test.wrapper.auditCall", 250)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Audit

/** Parallel-safe call log; provided by the caller as a context parameter. */
class AuditTrail {
    val methods = CopyOnWriteArrayList<MethodMeta>()
    private val recorded = CopyOnWriteArrayList<String>()

    val entries: List<String>
        get() = recorded.toList()

    fun record(entry: String) {
        recorded.add(entry)
    }
}

/** `@Audit` runtime helper — same shape contract as the built-in wrappers. */
context(trail: AuditTrail)
suspend fun <T> auditCall(
    meta: MethodMeta,
    call: suspend () -> T,
): T {
    trail.methods.add(meta)
    trail.record("${meta.owner}.${meta.name}")
    return call()
}
