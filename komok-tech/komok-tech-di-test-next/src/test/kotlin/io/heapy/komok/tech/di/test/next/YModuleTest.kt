package io.heapy.komok.tech.di.test.next

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class YModuleTest {
    @Test
    fun `test default create module`() {
        val yModule = createYModule {}

        val output = yModule.yService.map("Output: ")

        assertEquals(
            "Output: YAB",
            output
        )
    }

    @Test
    @Disabled("Not implemented")
    fun `test override service create module`() {
        val yModule = createYModule {
            yService {
                YService(
                    "Y1",
                    TODO("Service from a different module can't be reached, to use as dependency")
                )
            }
        }

        val output = yModule.yService.map("Output: ")

        assertEquals(
            "Output: Y1AB",
            output
        )
    }

    @Test
    fun `test empty override create module`() {
        val yModule = createYModule {
            aModule {}
            bModule {}
        }

        val output = yModule.yService.map("Output: ")

        assertEquals(
            "Output: YAB",
            output
        )
    }

    @Test
    fun `test one override create module`() {
        val yModule = createYModule {
            aModule {
                aStr { "A1" }
            }
            bModule {}
        }

        val output = yModule.yService.map("Output: ")

        assertEquals(
            "Output: YA1B",
            output
        )
    }

    @Test
    fun `test one inner override create module`() {
        val yModule = createYModule {
            aModule {}
            bModule {
                bStr { "B1" }
            }
        }

        val output = yModule.yService.map("Output: ")

        assertEquals(
            "Output: YAB1",
            output
        )
    }

    @Test
    fun `test two overrides create module`() {
        val yModule = createYModule {
            aModule {
                aStr { "A1" }
            }
            bModule {
                bStr { "B1" }
            }
        }

        val output = yModule.yService.map("Output: ")

        assertEquals(
            "Output: YA1B1",
            output
        )
    }
}
