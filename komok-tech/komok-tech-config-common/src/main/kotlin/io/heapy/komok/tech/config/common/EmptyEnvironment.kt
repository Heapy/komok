package io.heapy.komok.tech.config.common

object EmptyEnvironment : Environment {
    override val properties: Map<String, String> = emptyMap()
}
