package io.heapy.vipassana.migration

import io.heapy.vipassana.database.DatabaseContext
import kotlinx.serialization.Serializable
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.streams.toList

context(DatabaseContext)
fun executeMigrations(config: MigrationConfig) {
}

private fun MigrationConfig.listFsMigrations(): List<String> =
    Files.list(Path(path))
        .map { it.fileName.toString() }
        .toList()

context(DatabaseContext)
private fun MigrationConfig.listDbMigrations(): List<String> =
   TODO("SELECT name FROM migrations")

@Serializable
data class MigrationConfig(
    val path: String,
)

fun main() {
    MigrationConfig("migrations")
        .listFsMigrations()
        .let { println(it) }
}
