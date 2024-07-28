package io.heapy.komok.infra.di

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private val module1 by module {
    provideInstance<String> { "Hello" }
}

private val module2 by module {
    provideInstance<String> { "World" }
    dependency(module1)
}

private val withDuplication by module {
    provideInstance<String> { "Hello" }
    provideInstance<String> { "World" }
}

class DuplicateBindingTest {
    @Test
    fun `duplicate binding in module for same key should throw error`() =
        runTest {
            val exception = assertThrows<ContextException> {
                createContextAndGet(
                    type = genericKey<String>(),
                    moduleProvider = withDuplication,
                )
            }

            assertEquals(
                """
                Binding [kotlin.String] duplicated in module [class io.heapy.komok.infra.di.DuplicateBindingTestKt.withDuplication].
                """.trimIndent(),
                exception.message,
            )
        }

    @Test
    fun `duplicate binding for same key should throw error`() =
        runTest {
            val exception = assertThrows<ContextException> {
                createContextAndGet(
                    genericKey<String>(),
                    module2,
                )
            }

            assertEquals(
                """
                Binding [kotlin.String] already present in module [class io.heapy.komok.infra.di.DuplicateBindingTestKt.module2]. Current module: [class io.heapy.komok.infra.di.DuplicateBindingTestKt.module1]
                """.trimIndent(),
                exception.message,
            )
        }
}
