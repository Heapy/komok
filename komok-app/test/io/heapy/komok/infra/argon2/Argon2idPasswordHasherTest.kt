package io.heapy.komok.infra.argon2

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.random.RandomGenerator

class Argon2idPasswordHasherTest {
    @Test
    fun hash() = runTest {
        val graph = createPasswordHasherModuleGraph().fork {
            passwordHasherModule {
                replaceRandomGenerator(RandomGenerator { 4 })
            }
        }

        val hash = graph.passwordHasher().hash("password")
        assertEquals(
            """{"hash":"AUYsuzTXtmMHNvSjhZMcxvaX4i097sBWy8LmYh0ogZI=","salt":[4,0,0,0,0,0,0,0,4,0,0,0,0,0,0,0]}""",
            hash,
        )
    }

    @Test
    fun verify() = runTest {
        val graph = createPasswordHasherModuleGraph().fork {
            passwordHasherModule {
                replaceRandomGenerator(RandomGenerator { 4 })
            }
        }

        val result = graph.passwordHasher().verify(
            password = "password",
            hash = """{"hash":"AUYsuzTXtmMHNvSjhZMcxvaX4i097sBWy8LmYh0ogZI=","salt":[4,0,0,0,0,0,0,0,4,0,0,0,0,0,0,0]}""",
        )

        assertTrue(result)
    }
}
