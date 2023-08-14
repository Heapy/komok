package io.heapy.vipassana.configuration

import java.io.File

class Env private constructor(
    private val env: Map<String, String>,
) {
    operator fun get(
        key: String,
    ): String =
        env[key]
            ?: error("Configuration key $key not found")

    fun getOrNull(
        key: String,
    ): String? =
        env[key]

    fun <T> getOrDefault(
        key: String,
        converter: (String) -> T,
        default: T,
    ) = env[key]
        ?.let(converter)
        ?: default

    fun getOrDefault(
        key: String,
        default: String,
    ) = env[key]
        ?: default

    companion object {
        fun new(
            system: Map<String, String> = System.getenv(),
            overrides: Map<String, String> = emptyMap(),
        ): Env = buildMap {
            putAll(system)
            putAll(file)
            putAll(overrides)
        }.let(::Env)

        private val file: Map<String, String>
            get() = buildMap {
                val file = File(".env")
                if (file.exists()) {
                    file.forEachLine { line ->
                        val (key, value) = line.split("=", limit = 2)
                        put(key.trim(), value.trim())
                    }
                }
            }
    }
}
