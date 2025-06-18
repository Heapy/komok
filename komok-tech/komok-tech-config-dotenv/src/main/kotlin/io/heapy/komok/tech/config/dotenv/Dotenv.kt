package io.heapy.komok.tech.config.dotenv

import io.heapy.komok.tech.config.common.DefaultEnvironment
import io.heapy.komok.tech.config.common.EmptyEnvironment
import io.heapy.komok.tech.config.common.Environment
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines

/**
 * Load environment variables from `.env` file.
 * In case of multiple `.env` files, the closest one to the working directory is used.
 * This function will traverse the directory tree up to the folder with `.git`.
 */
fun dotenv(
    configurationBuilder: DotenvConfigurationBuilder.() -> Unit = {},
): Environment {
    val configuration: DotenvConfiguration = DotenvConfigurationBuilder().also(configurationBuilder).copy()
    val workingDirectory = Paths
        .get(".")
        .toAbsolutePath()
        .normalize()
    return resolveEnv(configuration, workingDirectory)
        ?.let { envPath ->
            envPath
                .readLines()
                .filter(String::isNotBlank)
                .filter { !it.startsWith(configuration.comment) }
                .associate {
                    val split = it.split(
                        "=",
                        limit = 2,
                    )
                    if (split.size != 2) error("Line syntax: key=value, got: $it")
                    split[0].trim() to split[1].trim()
                }
        }
        ?.let(::DefaultEnvironment)
        ?: EmptyEnvironment
}

private fun resolveEnv(
    configuration: DotenvConfiguration,
    root: Path,
): Path? {
    return root
        .resolve(configuration.envFileName)
        .let { envPath ->
            when {
                envPath.exists() && envPath.isRegularFile() -> envPath
                configuration.traversalStopCondition(root) -> null
                root.parent == null -> null
                else -> resolveEnv(
                    configuration = configuration,
                    root = root.parent,
                )
            }
        }
}
