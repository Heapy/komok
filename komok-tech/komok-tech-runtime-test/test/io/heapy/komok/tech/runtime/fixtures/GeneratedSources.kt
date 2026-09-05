package io.heapy.komok.tech.runtime.fixtures

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal fun generatedKspMainSourceRoot(
    moduleName: String = "komok-tech-runtime-test",
): Path {
    val root = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .first { Files.exists(it.resolve("project.yaml")) }
    val sources = root.resolve("build/generated/$moduleName/main/src/ksp/kotlin")
    check(Files.isDirectory(sources)) { "KSP generated source root not found: $sources" }
    return sources
}
