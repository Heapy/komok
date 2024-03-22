package io.heapy.komok.jdun

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import kotlin.reflect.typeOf

/**
 * Nice to have for qualifier support without extra methods calls.
 *
 * https://youtrack.jetbrains.com/issue/KT-29919
 */
internal class GenericKeyAnnotationTest {
    @Target(AnnotationTarget.TYPE)
    annotation class Foo

    @Test
    fun `annotation not preserved`() {
        assertThrows<AssertionFailedError> {
            assertEquals(
                1,
                typeOf<@Foo String>().annotations.size,
            )
        }
    }
}
