package io.heapy.komok.tech.di.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BModuleTest {
    @Test
    fun `test default create module`() {
        val bModule = createBModule {}

        val output = bModule.bService.map("Output: ")

        assertEquals(
            "Output: B",
            output
        )
    }
}
