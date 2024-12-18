package io.heapy.komok.tech.config.dotenv

import java.nio.file.Path

interface DotenvConfiguration {
    val envFileName: String
    val comment: String
    val traversalStopCondition: (Path) -> Boolean
}
