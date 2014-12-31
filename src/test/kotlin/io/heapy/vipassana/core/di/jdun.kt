package io.heapy.vipassana.core.di

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.reflect
import kotlin.reflect.typeOf

/**
 * Nice to have for qualifier support without extra DSL.
 *
 * https://youtrack.jetbrains.com/issue/KT-29919
 */
class GenericTypeAnnotationTest {
    @Target(AnnotationTarget.TYPE)
    annotation class Foo

    @Test
    fun `test annotation preserved`() {
        assertThrows<AssertionFailedError> {
            assertEquals(1, typeOf<@Foo String>().annotations.size)
        }
    }
}

/**
 * Nice to have for creating instance-factories as lambdas.
 */
class LambdaReflectionTest {
    @Test
    fun `introspecting lambdas`() {
        val lambda = { a: String, b: Int -> a + b }

        // Since feature not yet implemented in Kotlin
        assertThrows<KotlinReflectionInternalError> {
            val result = lambda.reflect()?.call("Hello ", 42)
            assertEquals("Hello 42", result)
        }
    }
}
