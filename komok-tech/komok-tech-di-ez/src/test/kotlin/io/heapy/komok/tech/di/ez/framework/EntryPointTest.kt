package io.heapy.komok.tech.di.ez.framework

import io.heapy.komok.tech.di.ez.impl.ContextException
import io.heapy.komok.tech.di.ez.dsl.module
import io.heapy.komok.tech.di.ez.dsl.provide
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EntryPointTest {
    @Test
    fun `test inline module`() =
        runTest {
            assertFalse(TestInlineModule.executed)

            komok<TestInlineModule, Unit> {
                provide(::TestInlineModule)
                provide(::Service1)
                provide(::Service2)
            }

            assertTrue(TestInlineModule.executed)
        }

    class TestInlineModule(
        private val service1: Service1,
    ) : EntryPoint<Unit> {
        override suspend fun run() {
            service1.run()
            executed = true
        }

        companion object {
            var executed = false
        }
    }

    @Test
    fun `test inline module no entrypoint`() =
        runTest {
            val exception = assertThrows<ContextException> {
                komok<EntryPoint<Unit>, Unit> {
                    provide(::TestInlineModule)
                    provide(::Service1)
                    provide(::Service2)
                }
            }

            assertEquals(
                "Required io.heapy.komok.tech.di.ez.framework.EntryPoint<kotlin.Unit> not found in context.",
                exception.message,
            )
        }

    @Test
    fun `test module`() =
        runTest {
            assertFalse(TestModule.executed)

            komok<TestModule, Unit> {
                provide(::TestModule)
                dependency(k1)
            }

            assertTrue(TestModule.executed)
        }

    class TestModule(
        private val service1: Service1,
    ) : EntryPoint<Unit> {
        override suspend fun run() {
            service1.run()
            executed = true
        }

        companion object {
            var executed = false
        }
    }

    class Service1(
        private val service2: Service2,
    ) {
        fun run() {
            service2.hello()
        }
    }

    class Service2 {
        fun hello(): String {
            return "Hello, World!"
        }
    }

    private val k1 by module {
        dependency(k2)
        provide(::Service1)
    }

    private val k2 by module {
        provide(::Service2)
    }
}
