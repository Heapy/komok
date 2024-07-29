package io.heapy.komok.tech.di.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AModuleTest {
    @Test
    fun `test default create module`() {
        val aModule = createAModule {}

        val output = aModule.aService.map("Output: ")

        assertEquals(
            "Output: AB",
            output
        )
    }

    @Test
    fun `test default override create module`() {
        val aModule = createAModule {
            bModule {
            }
        }

        val output = aModule.aService.map("Output: ")

        assertEquals(
            "Output: AB",
            output
        )
    }
}
