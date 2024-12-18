package io.heapy.komok.tech.config.dotenv

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

data class DotenvConfigurationBuilder(
    override val envFileName: String = ".env",
    override var comment: String = "# ",
    override var traversalStopCondition: (Path) -> Boolean = { path ->
        val gitPath = path.resolve(".git")
        gitPath.exists() && gitPath.isDirectory()
    },
) : DotenvConfiguration
