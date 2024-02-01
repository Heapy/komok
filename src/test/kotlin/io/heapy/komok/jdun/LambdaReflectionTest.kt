package io.heapy.komok.jdun

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.reflect

/**
 * @author Ruslan Ibrahimau
 */
internal class LambdaReflectionTest {
    @OptIn(ExperimentalReflectionOnLambdas::class)
    @Test
    fun `lambda cannot be called`() {
        val lambda = { a: String, b: Int -> a + b }

        // Since feature not yet implemented in Kotlin
        assertThrows<KotlinReflectionInternalError> {
            val reflection = lambda.reflect()
            reflection?.call("Hello ", 42)
        }
    }
}
