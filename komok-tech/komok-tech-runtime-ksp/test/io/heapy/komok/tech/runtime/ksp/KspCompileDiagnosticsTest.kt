package io.heapy.komok.tech.runtime.ksp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

/**
 * Runs real Kotlin Toolchain/KSP compilations against tiny fixtures. These tests cover diagnostics
 * that pure helper tests cannot prove: KSP must fail the compilation and surface the
 * human-readable processor message in compiler output.
 */
class KspCompileDiagnosticsTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `missing service binding fails compilation with dependency chain and exact generic key`() {
        val output = compileAndFail(
            """
            package example

            import io.heapy.komok.tech.runtime.Service

            @Service
            fun stringItems(): List<String> =
                listOf("a")

            @Service
            class NeedsIntItems(
                val items: List<Int>,
            )
            """.trimIndent(),
        )

        assertTrue(
            output.contains(
                "No @Service binding found for dependency `items: kotlin.collections.List<kotlin.Int>`. " +
                    "Dependency chain: example.AppServices.needsIntItems(kotlin.collections.List<kotlin.Int>) " +
                    "-> items:kotlin.collections.List<kotlin.Int>.",
            ),
            output,
        )
    }

    @Test
    fun `duplicate service binding fails compilation with both candidate addresses`() {
        val output = compileAndFail(
            """
            package example

            import io.heapy.komok.tech.runtime.Service

            data class Clock(val name: String)

            @Service
            fun systemClock(): Clock =
                Clock("system")

            @Service
            fun testClock(): Clock =
                Clock("test")

            @Service
            class ClockConsumer(
                val clock: Clock,
            )
            """.trimIndent(),
        )

        assertTrue(
            output.contains("Multiple @Service bindings found for dependency `clock: example.Clock`."),
            output,
        )
        assertTrue(output.contains("example.AppServices.systemClock"), output)
        assertTrue(output.contains("example.AppServices.testClock"), output)
    }

    @Test
    fun `module cycle fails compilation with module graph diagnostic`() {
        val output = compileAndFail(
            """
            package example

            import io.heapy.komok.tech.runtime.Module

            @Module
            open class A(
                private val b: B,
            )

            @Module
            open class B(
                private val a: A,
            )
            """.trimIndent(),
        )

        assertTrue(output.contains("Module graph has at least one cycle"), output)
        assertTrue(output.contains("A=[B]"), output)
        assertTrue(output.contains("B=[A]"), output)
    }

    private fun compileAndFail(source: String): String {
        writeFixture(source)
        val root = repoRoot()
        val command = if (System.getProperty("os.name").startsWith("Windows")) {
            listOf("cmd", "/c", root.resolve("kotlin.bat").absolutePathString())
        } else {
            listOf(root.resolve("kotlin").absolutePathString())
        }
        val process = ProcessBuilder(
            command + listOf("build", "--project-dir=${tempDir.absolutePathString()}", "-m", "fixture"),
        )
            .directory(tempDir.toFile())
            .redirectErrorStream(true)
            .start()
        val outputReader = CompletableFuture.supplyAsync {
            process.inputStream.bufferedReader().use { it.readText() }
        }
        val finished = process.waitFor(2, TimeUnit.MINUTES)
        if (!finished) {
            process.destroyForcibly()
            val output = outputReader.get(10, TimeUnit.SECONDS)
            fail<String>("Nested Kotlin Toolchain build timed out:\n$output")
        }
        val output = outputReader.get(10, TimeUnit.SECONDS)
        assertTrue(process.exitValue() != 0, output)
        return output
    }

    private fun writeFixture(source: String) {
        val root = repoRoot()
        // Use the repository's compiler, KSP and library versions, without depending
        // on Gradle caches, prebuilt jars or a prior publication to Maven Local.
        Files.copy(root.resolve("libs.versions.toml"), tempDir.resolve("libs.versions.toml"))
        Files.createDirectories(tempDir.resolve("templates"))
        for (template in listOf("common", "ksp")) {
            val path = "templates/$template.module-template.yaml"
            Files.copy(root.resolve(path), tempDir.resolve(path))
        }
        writeFile("project.yaml", "modules: [fixture, processor, runtime]")
        writeFile(
            "processor/module.yaml",
            """
            product: jvm/lib
            apply: [//templates/common.module-template.yaml]
            dependencies:
              - ${'$'}libs.kotlinpoet.ksp: exported
              - ${'$'}libs.ksp: exported
            """.trimIndent(),
        )
        writeFile(
            "runtime/module.yaml",
            """
            product:
              type: kmp/lib
              platforms: [jvm]
            apply: [//templates/common.module-template.yaml]
            dependencies:
              - ${'$'}libs.kotlinx.coroutines.core: exported
            settings:
              kotlin:
                freeCompilerArgs: [-Xexpect-actual-classes]
            """.trimIndent(),
        )
        writeFile(
            "fixture/module.yaml",
            """
            product: jvm/lib
            apply:
              - //templates/common.module-template.yaml
              - //templates/ksp.module-template.yaml
            dependencies:
              - //runtime
            settings:
              kotlin:
                ksp:
                  processors: [//processor]
                  processorOptions:
                    komok.providers.module: example.AppServices
            """.trimIndent(),
        )
        for (directory in listOf("src", "resources")) {
            copySources(root.resolve("komok-tech/komok-tech-runtime-ksp/$directory"), "processor/$directory")
        }
        for (directory in listOf("src", "src@jvm")) {
            copySources(root.resolve("komok-tech/komok-tech-runtime/$directory"), "runtime/$directory")
        }
        writeFile("fixture/src/example/Sample.kt", source)
    }

    private fun writeFile(relativePath: String, content: String) {
        val path = tempDir.resolve(relativePath)
        Files.createDirectories(path.parent)
        Files.writeString(path, content + "\n")
    }

    private fun copySources(source: Path, destination: String) {
        Files.walk(source).use { paths ->
            paths.filter { Files.isRegularFile(it) }.forEach { file ->
                val target = tempDir.resolve(destination).resolve(source.relativize(file))
                Files.createDirectories(target.parent)
                Files.copy(file, target)
            }
        }
    }

    private fun repoRoot(): Path =
        generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
            .first {
                Files.exists(it.resolve("project.yaml")) &&
                    Files.isDirectory(it.resolve("komok-tech"))
            }
}
