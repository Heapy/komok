package io.heapy.vipassana.core.di

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApplicationTest {
    @Test
    fun `test inline module`() = runTest {
        val result = application<Application, String> {
            provide(::Application)
            provide(::Service1)
            provide(::Service2)
        }

        assertEquals("Hello, World!", result)
    }

    @Test
    fun `test module`() = runTest {
        val result = application<Application, String> {
            provide(::Application)
            dependency(k1)
        }

        assertEquals("Hello, World!", result)
    }

    class Application(
        private val service1: Service1
    ) : EntryPoint<String> {
        override suspend fun run(): String {
            return service1.run()
        }
    }

    class Service1(
        private val service2: Service2
    ) {
        fun run(): String {
            return service2.hello()
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

class DiTest {
    @Test
    fun `test inline module`() = runTest {
        application<Application, Unit> {
            provide(::Application)
            provide(::Service1)
            provide(::Service2)
        }
    }

    @Test
    fun `test module`() = runTest {
        application<Application, Unit> {
            provide(::Application)
            dependency(k1)
        }
    }

    class Application(
        private val service1: Service1
    ) : EntryPoint<Unit> {
        override suspend fun run() {
            service1.run()
        }
    }

    class Service1(
        private val service2: Service2
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
