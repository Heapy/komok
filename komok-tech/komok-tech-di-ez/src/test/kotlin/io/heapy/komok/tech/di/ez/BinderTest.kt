package io.heapy.komok.tech.di.ez

import io.heapy.komok.tech.di.ez.api.ModuleProvider
import io.heapy.komok.tech.di.ez.api.Provider
import io.heapy.komok.tech.di.ez.api.createContext
import io.heapy.komok.tech.di.ez.api.genericKey
import io.heapy.komok.tech.di.ez.api.get
import io.heapy.komok.tech.di.ez.dsl.module
import io.heapy.komok.tech.di.ez.dsl.provide
import io.heapy.komok.tech.di.ez.dsl.provideInstance
import io.heapy.komok.tech.di.ez.impl.ContextException
import io.heapy.komok.tech.di.ez.impl.isProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BasicBinderTest {
    interface Test1 {
        fun get(): String
    }

    class Test1Impl(
        private val test2: Test2,
    ) : Test1 {
        override fun get() =
            "Test1Impl ${test2.get()}"
    }

    interface Test2 {
        fun get(): String
    }

    class Test2Impl(
        private val test3: Test3,
    ) : Test2 {
        override fun get() =
            "Test2Impl ${test3.get()}"
    }

    class Test3 {
        fun get() =
            "Test3"
    }

    fun test2Provider(test3: Test3): Test2 {
        return Test2Impl(test3)
    }

    class TestRoot(
        val t1: Test1,
        val test2: Test2,
    ) {
        fun run() =
            "${t1.get()} ${test2.get()}"
    }

    private val module1 by module {
        provide<Test1>(::Test1Impl)
        provide(::test2Provider)
    }

    private val module2 by module {}

    private val module3 by module {
        provide(::Test3)
        provide(::TestRoot)
    }

    @Test
    fun test() =
        runTest {
            val module by module {
                dependency(module1)
                dependency(module2)
                dependency(module3)
            }

            val instanceByProvider = module
                .createContext()
                .get<Provider<TestRoot>>()
                .get()

            assertEquals(
                "Test1Impl Test2Impl Test3 Test2Impl Test3",
                instanceByProvider.run(),
            )
        }
}

class SingletonBinderTest {
    class Test1

    class TestRoot(
        val t1: Test1,
        val t2: Test1,
    )

    @Test
    fun test() =
        runTest {
            val module by module {
                provide(::Test1)
                provide(::TestRoot)
            }

            val root = module
                .createContext()
                .get<TestRoot>()

            assertSame(
                root.t1,
                root.t2,
            )
        }
}

class SingletonZeroArgBinderTest {
    class Test1

    class TestRoot(
        val t1: Test1,
        val t2: Test1,
    )

    @Test
    fun test() =
        runTest {
            val module by module {
                provideInstance(Test1())
                provide(::TestRoot)
            }

            val root = module
                .createContext()
                .get<TestRoot>()

            assertSame(
                root.t1,
                root.t2,
            )
        }
}

class OptionalInjectionTest {
    class Foo(
        val bar: Bar?,
    )

    class Bar

    private val module1 by module {
        provide(::Foo)
    }

    @Test
    fun `test constructor`() =
        runTest {
            val foo = module1
                .createContext()
                .get<Foo>()

            assertNull(foo.bar)
        }

    fun createFoo(bar: Bar?): Foo {
        return Foo(bar)
    }

    private val module2 by module {
        provide(::createFoo)
    }

    @Test
    fun `test provider`() =
        runTest {
            val foo = module2
                .createContext()
                .get<Foo>()

            assertNull(foo.bar)
        }
}

/**
 * ## Cyclic Dependencies
 *
 * We doesn't support cyclic dependencies,
 * instead of "hacking" classes thought proxies,
 * setters and field injections
 * we require our user to fix their architecture.
 */
class CyclicDependencyTest {
    class Foo(val bar: Bar)
    class Bar(val baz: Baz)
    class Baz(val foo: Foo)

    private val cyclic by module {
        provide(::Foo)
        provide(::Bar)
        provide(::Baz)
    }

    @Test
    fun `test cyclic dependencies`() =
        runTest {
            val exception = assertThrows<ContextException> {
                cyclic
                    .createContext()
                    .get<Foo>()
            }

            assertEquals(
                $$"""
                A circular dependency found:
                class io.heapy.komok.tech.di.ez.CyclicDependencyTest$Foo implemented by provider [class io.heapy.komok.tech.di.ez.CyclicDependencyTest$cyclic$2$1] <-- Circular dependency starts here
                  class io.heapy.komok.tech.di.ez.CyclicDependencyTest$Bar implemented by provider [class io.heapy.komok.tech.di.ez.CyclicDependencyTest$cyclic$2$2]
                    class io.heapy.komok.tech.di.ez.CyclicDependencyTest$Baz implemented by provider [class io.heapy.komok.tech.di.ez.CyclicDependencyTest$cyclic$2$3]
                      class io.heapy.komok.tech.di.ez.CyclicDependencyTest$Foo implemented by provider [class io.heapy.komok.tech.di.ez.CyclicDependencyTest$cyclic$2$1]
                """.trimIndent(),
                exception.message,
            )
        }
}

class ObjectBindingTest {
    object ToBind

    private val objectModule by module {
        provideInstance(ToBind)
    }

    @Test
    fun `komok disallows object binding`() =
        runTest {
            val exception = assertThrows<ContextException> {
                objectModule
                    .createContext()
                    .get<ToBind>()
            }

            assertEquals(
                """
                Objects not allowed to be bound.
                """.trimIndent(),
                exception.message,
            )
        }
}

class CircularDependencyTest {
    private class HelloWorld

    private val module1: ModuleProvider by module {
        dependency(module3)
        provideInstance(HelloWorld())
    }

    private val module2 by module {
        dependency(module1)
    }

    private val module3 by module {
        dependency(module2)
    }

    @Test
    fun `circular dependency in modules shouldn't throw error`() {
        runTest {
            module3
                .createContext()
                .get<HelloWorld>()
        }
    }
}

class IsProviderUtilityTest {
    @Test
    fun `simple case`() {
        assertTrue(genericKey<Provider<String>>().isProvider())
        assertFalse(genericKey<String>().isProvider())
    }
}
