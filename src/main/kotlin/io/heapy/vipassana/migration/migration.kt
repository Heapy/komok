package io.heapy.vipassana.migration

import io.heapy.vipassana.database.DatabaseContext
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path

context(DatabaseContext)
fun executeMigrations(config: MigrationConfig) {
}

private fun MigrationConfig.listFsMigrations(): List<String> =
    Files.list(Path(path))
        .map { it.fileName.toString() }
        .toList()

private fun MigrationConfig.listClasspathMigrations(): List<String> {
    val classloader = Thread.currentThread().contextClassLoader
    val uri = classloader.getResource(path)
        ?.toURI()
        ?: return emptyList()
    return Files
        .walk(Paths.get(uri))
        .filter { Files.isRegularFile(it) }
        .map { it.toString() }
        .toList()
}

context(DatabaseContext)
private fun MigrationConfig.listDbMigrations(): List<String> =
   TODO("SELECT name FROM migrations")

@Serializable
data class MigrationConfig(
    val path: String,
)

fun main() {
    MigrationConfig("migrations")
        .listClasspathMigrations()
        .let { println(it) }
}
