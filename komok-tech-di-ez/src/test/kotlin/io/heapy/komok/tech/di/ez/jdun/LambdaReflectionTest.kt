package io.heapy.komok.tech.di.ez.jdun

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
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
        assertThrows<RuntimeException> {
            val reflection = lambda.reflect()
                ?: error("Reflection for lambda is null")
            reflection.call(
                "Hello ",
                42,
            )
        }
    }
}
