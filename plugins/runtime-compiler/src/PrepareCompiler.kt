package io.heapy.komok.build

import org.jetbrains.amper.plugins.CompilationArtifact
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@TaskAction
fun prepareCompiler(
    @Input compilerJar: CompilationArtifact,
    @Input(inferTaskDependency = false) configuredCompilerJar: Path,
    @Output sourcesDirectory: Path,
) {
    // Toolchain 0.13 passes -Xplugin as an untracked raw argument. In nested builds
    // (including BCV), verify that the configured JAR matches this build's plugin.
    // This prevents a successful check against stale compiler transformations.
    check(Files.mismatch(compilerJar.artifact, configuredCompilerJar) == -1L) {
        "The configured Komok compiler is stale. Run ./kotlin build --platform jvm from the project root first."
    }
    fun fingerprint(path: Path): String = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }
    Files.createDirectories(sourcesDirectory)
    Files.writeString(sourcesDirectory.resolve("KomokCompilerFingerprint.kt"),
        "// Komok compiler: ${fingerprint(compilerJar.artifact)}; configured: ${fingerprint(configuredCompilerJar)}\n")
}
