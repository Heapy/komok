package io.heapy.komok.business.user.argon2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.random.RandomGenerator

class Argon2idPasswordHasherTest {
    @Test
    fun hash() {
        val module = createPasswordHasherModule {
            randomGenerator {
                RandomGenerator { 4 }
            }
        }

        val hash = module.passwordHasher.hash("password")
        assertEquals(
            """{"hash":"AUYsuzTXtmMHNvSjhZMcxvaX4i097sBWy8LmYh0ogZI=","salt":[4,0,0,0,0,0,0,0,4,0,0,0,0,0,0,0]}""",
            hash,
        )
    }

    @Test
    fun verify() {
        val module = createPasswordHasherModule {
            randomGenerator {
                RandomGenerator { 4 }
            }
        }

        val result = module.passwordHasher.verify(
            password = "password",
            hash = """{"hash":"AUYsuzTXtmMHNvSjhZMcxvaX4i097sBWy8LmYh0ogZI=","salt":[4,0,0,0,0,0,0,0,4,0,0,0,0,0,0,0]}""",
        )

        assertTrue(result)
    }
}
