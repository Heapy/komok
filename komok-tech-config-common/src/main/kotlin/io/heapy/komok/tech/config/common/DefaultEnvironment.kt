package io.heapy.komok.tech.config.common

data class DefaultEnvironment(
    override val properties: Map<String, String>,
) : Environment
