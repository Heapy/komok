package io.heapy.komok.tech.di.ez

import io.heapy.komok.tech.di.ez.api.createContext
import io.heapy.komok.tech.di.ez.api.get
import io.heapy.komok.tech.di.ez.dsl.module
import io.heapy.komok.tech.di.ez.dsl.provideInstance
import io.heapy.komok.tech.di.ez.impl.ContextException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private val module1 by module {
    provideInstance("Hello")
}

private val module2 by module {
    provideInstance("World")
    dependency(module1)
}

private val withDuplication by module {
    provideInstance("Hello")
    provideInstance("World")
}

class DuplicateBindingTest {
    @Test
    fun `duplicate binding in module for same key should throw error`() =
        runTest {
            val exception = assertThrows<ContextException> {
                withDuplication
                    .createContext()
                    .get<String>()
            }

            assertEquals(
                """
                Binding [kotlin.String] duplicated in module [class io.heapy.komok.tech.di.ez.DuplicateBindingTestKt.withDuplication].
                """.trimIndent(),
                exception.message,
            )
        }

    @Test
    fun `duplicate binding for same key should throw error`() =
        runTest {
            val exception = assertThrows<ContextException> {
                module2
                    .createContext()
                    .get<String>()
            }

            assertEquals(
                """
                Binding [kotlin.String] already present in module [class io.heapy.komok.tech.di.ez.DuplicateBindingTestKt.module2]. Current module: [class io.heapy.komok.tech.di.ez.DuplicateBindingTestKt.module1]
                """.trimIndent(),
                exception.message,
            )
        }
}
