package io.heapy.komok.tech.runtime.ksp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * Verifies the pure module-validation helpers. The KSP-driven diagnostics (duplicate
 * module types, lifecycle-hook shape) are wired into [KomokRuntimeSymbolProcessor]; the
 * compile-error path needs a compile-testing harness (deferred), so the decision logic is
 * unit-tested here in isolation.
 */
class ModuleValidationTest {
    @Test
    fun `duplicate module types - flags only fqns that repeat`() {
        assertAll(
            { assertEquals(emptySet<String>(), duplicateModuleTypes(emptyList())) },
            { assertEquals(emptySet<String>(), duplicateModuleTypes(listOf("a.A", "a.B", "a.C"))) },
            { assertEquals(setOf("a.A"), duplicateModuleTypes(listOf("a.A", "a.A", "a.B"))) },
            {
                assertEquals(
                    setOf("a.A", "a.B"),
                    duplicateModuleTypes(listOf("a.A", "a.B", "a.A", "a.B", "a.C")),
                )
            },
        )
    }
}
