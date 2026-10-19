package io.heapy.komok.tech.runtime.compiler

import io.heapy.komok.tech.runtime.KomokRuntime
import kotlinx.coroutines.Job
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path

/** Real frontend/backend compilations, with the same compiler and runtime as this build. */
class CompilerDiagnosticsTest {
    @TempDir lateinit var directory: Path

    @Test fun `missing exact generic dependency reports its chain`() {
        fails("""
            @Service fun strings(): List<String> = listOf("a")
            @Service class Consumer(val items: List<Int>)
            val graph = services()
        """, "No binding for items: kotlin.collections.List<kotlin.Int>", "example.Services.consumer")
    }

    @Test fun `ambiguous dependency reports both providers`() {
        fails("""
            class Clock
            @Service fun systemClock(): Clock = Clock()
            @Service fun testClock(): Clock = Clock()
            @Service class Consumer(val clock: Clock)
            val graph = services()
        """, "Multiple bindings", "example.Services.systemClock", "example.Services.testClock")
    }

    @Test fun `module cycle is rejected in FIR without a graph call`() {
        fails("""
            @Module class A(val b: B)
            @Module class B(val a: A)
        """, "Komok module cycle", "example.A", "example.B")
    }

    @Test fun `provider requires the lexical runtime context`() {
        fails("""
            @Module class Example { suspend fun value(): String = "value" }
        """, "context(_: KomokContext)")
    }

    @Test fun `invalid lifecycle shape is rejected in FIR`() {
        fails("""
            @Module class Example { @Startup fun start(): String = "invalid" }
        """, "lifecycle hooks must be public suspend functions returning Unit")
    }

    @Test fun `mock requires an interface`() {
        fails("""
            class Concrete
            val value = mock<Concrete>()
        """, "mocks require an interface")
    }

    @Test fun `interface providers need a default implementation`() {
        fails("""
            @Module interface Example {
                context(_: KomokContext) suspend fun value(): String
            }
        """, "providers must have a body")
    }

    @Test fun `incompatible explicit service binding is rejected`() {
        fails("""
            interface Contract
            @Service(bind = Contract::class) class Implementation
            val graph = services()
        """, "does not implement example.Contract")
    }

    @Test fun `sealed mock target is rejected before backend generation`() {
        fails("""
            sealed interface Closed
            val value = mock<Closed>()
        """, "cannot implement sealed interfaces")
    }

    @Test fun `mock selector must select the receiver instead of capturing a variable`() {
        fails("""
            interface Value { val text: String }
            fun example() {
                val text = "captured"
                mock<Value>().every { text }.returns("stub")
            }
        """, "selector must contain exactly one method or property access")
    }

    @Test fun `service dependency cycles fail before executing the graph`() {
        fails("""
            @Service class A(val b: B)
            @Service class B(val a: A)
            val graph = services()
        """, "Komok dependency cycle", "example.Services.a", "example.Services.b")
    }

    private fun fails(source: String, vararg expected: String) {
        val file = directory.resolve("Fixture.kt")
        Files.writeString(file, "package example\nimport io.heapy.komok.tech.runtime.*\n" + source.trimIndent())
        fun location(type: Class<*>): String = Path.of(type.protectionDomain.codeSource.location.toURI()).toString()
        val classpath = listOf(KomokRuntime::class.java, Job::class.java, Unit::class.java)
            .map(::location).distinct().joinToString(File.pathSeparator)
        val output = ByteArrayOutputStream()
        val result = PrintStream(output).use { stream -> K2JVMCompiler().exec(stream,
            "-classpath", classpath, "-no-stdlib", "-no-reflect", "-jvm-target", "17",
            "-Xverify-ir=error",
            "-Xplugin=${location(KomokCompilerPluginRegistrar::class.java)}",
            "-d", directory.resolve("classes").toString(), file.toString()) }
        val text = output.toString()
        assertEquals(ExitCode.COMPILATION_ERROR, result, text)
        expected.forEach { assertTrue(text.contains(it, ignoreCase = true), text) }
        assertFalse(text.contains("IrGenerationExtensionException"), text)
    }
}
