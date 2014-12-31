package io.heapy.vipassana.core.di

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import kotlin.reflect.typeOf

class DelegationPatternBinderTest {
    interface Parent
    interface DelegateToParent : Parent
    class ParentImplementation : DelegateToParent

    @Test
    @Disabled
    internal fun test() = runTest {
        val moduleDelegation by module {
            implementBy<Parent, DelegateToParent>()
            provide<DelegateToParent>(DelegationPatternBinderTest::ParentImplementation)
        }

        val ie1 = createContextAndGet(
            type<Parent>(),
            moduleDelegation
        )

        assertTrue(ie1 is ParentImplementation)
    }
}

class BasicBinderTest {
    interface Test1 {
        fun get(): String
    }

    class Test1Impl(
        private val test2: Test2
    ) : Test1 {
        override fun get() = "Test1Impl ${test2.get()}"
    }

    interface Test2 {
        fun get(): String
    }

    class Test2Impl(
        private val test3: Test3
    ) : Test2 {
        override fun get() = "Test2Impl ${test3.get()}"
    }

    class Test3 {
        fun get() = "Test3"
    }

    suspend fun test2Provider(test3: Test3): Test2 {
        return Test2Impl(test3)
    }

    class TestRoot(
        val t1: Test1,
        val test2: Test2
    ) {
        fun run() = "${t1.get()} ${test2.get()}"
    }

    private val module1 by module {
        provide<Test1>(BasicBinderTest::Test1Impl)
        provide(::test2Provider)
    }

    private val module2 by module {}

    private val module3 by module {
        provide(BasicBinderTest::Test3)
        provide(BasicBinderTest::TestRoot)
    }

    @Test
    fun test() = runTest {
        val module by module {
            dependency(module1)
            dependency(module2)
            dependency(module3)
        }

        val testProvider = createContextAndGet(
            type<Provider<TestRoot>>(),
            module
        )

        val test = testProvider.new()

        assertEquals(
            "Test1Impl Test2Impl Test3 Test2Impl Test3",
            test.run()
        )
    }
}

class OptionalInjectionTest {
    class Foo(
        val bar: Bar?
    )

    class Bar

    private val module1 by module {
        provide(OptionalInjectionTest::Foo)
    }

    @Test
    fun `test constructor`() = runTest {
        val foo = createContextAndGet(
            type<Foo>(),
            module1
        )

        assertNull(foo.bar)
    }

    fun createFoo(bar: Bar?): Foo {
        return Foo(bar)
    }

    val module2 by module {
        provide(::createFoo)
    }

    @Test
    fun `test provider`() = runTest {
        val foo = createContextAndGet(
            type<Foo>(),
            module2
        )

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

    val cyclic by module {
        provide(CyclicDependencyTest::Foo)
        provide(CyclicDependencyTest::Bar)
        provide(CyclicDependencyTest::Baz)
    }

    @Test
    fun `test cyclic dependencies`() = runTest {
        val exception = assertThrows<ContextException> {
            runBlocking {
                createContextAndGet(
                    type<Foo>(),
                    cyclic
                )
            }
        }

        assertEquals("""
        A circular dependency found: 
        class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}Foo implemented by provider [class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}cyclic${'$'}2${'$'}1] <-- Circular dependency starts here
          class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}Bar implemented by provider [class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}cyclic${'$'}2${'$'}2]
            class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}Baz implemented by provider [class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}cyclic${'$'}2${'$'}3]
              class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}Foo implemented by provider [class io.heapy.vipassana.core.di.CyclicDependencyTest${'$'}cyclic${'$'}2${'$'}1]
        """.trimIndent(), exception.message)
    }
}

class ObjectBindingTest {
    object ToBind

    private val obj by module {
        provide({ ToBind })
    }

    @Test
    fun `di disallows object binding`() = runTest {
        val exception = assertThrows<ContextException> {
            runBlocking {
                createContextAndGet(
                    type<ToBind>(),
                    obj
                )
            }
        }

        assertEquals("""
            Objects not allowed to bind
        """.trimIndent(), exception.message)
    }
}

class DuplicateBindingTest {
    private val withDuplication by module {
        provide({ "Hello" })
        provide({ "World" })
    }

    @Test
    fun `duplicate binding in module for same key should throw error`() {
        val exception = assertThrows<ContextException> {
            runTest {
                createContextAndGet(
                    type<String>(),
                    withDuplication
                )
            }
        }

        assertEquals("""
            Binding duplicated in module [class io.heapy.vipassana.core.di.DuplicateBindingTest.withDuplication].
        """.trimIndent(), exception.message)
    }

    private val module1 by module {
        provide({ "Hello" })
    }

    private val module2 by module {
        provide({ "World" })
        dependency(module1)
    }

    @Test
    fun `duplicate binding for same key should throw error`() {
        val exception = assertThrows<ContextException> {
            runTest {
                createContextAndGet(
                    type<String>(),
                    module2
                )
            }
        }

        assertEquals("""
            Binding already present in module [class io.heapy.vipassana.core.di.DuplicateBindingTest.module2]. Current module: [class io.heapy.vipassana.core.di.DuplicateBindingTest.module1]
        """.trimIndent(), exception.message)
    }
}

class CircularDependencyTest {
    private class HelloWorld

    private val module1: ModuleProvider by module {
        dependency(module3)
        provide({ HelloWorld() })
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
            createContextAndGet(
                type<HelloWorld>(),
                module3
            )
        }
    }
}

class IsProviderUtilityTest {
    @Test
    fun `simple case`() {
        assertTrue(Key(typeOf<Provider<String>>()).isProvider())
        assertFalse(Key(typeOf<String>()).isProvider())
    }
}
