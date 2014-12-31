package io.heapy.vipassana.config

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Wrapper for [System.getenv], for easy testing and extensibility.
 */
interface Env {
    val vars: Map<String, String>

    /**
     * Throws exception if env not found
     */
    fun require(env: String): String =
        get(env)
            ?: error("$env not defined.")

    fun getOrDefault(env: String, default: String): String =
        get(env)
            ?: default

    /**
     * Returns null, if env not found
     */
    operator fun get(env: String): String?
}

class Dotenv private constructor(
    private val lines: List<String>,
    private val system: Map<String, String>,
) : Env {
    override val vars =
        getEnvironmentVariables(
            lines = lines,
            system = system,
        )

    override fun get(env: String) =
        vars[env]

    companion object {
        fun new() =
            Paths.get(".env").let { file ->
                Dotenv(
                    lines = if (Files.exists(file)) Files.readAllLines(file) else emptyList(),
                    system = System.getenv(),
                )
            }

        internal fun getEnvironmentVariables(
            lines: List<String>,
            system: Map<String, String>,
        ) =
            system + lines
                .mapIndexed { index, line ->
                    index to line
                }
                .filterNot { (_, line) -> line.startsWith("#") }
                .filterNot { (_, line) -> line.isEmpty() }
                .associate { (idx, line) ->
                    if (line.contains("=")) {
                        val (name, value) = line.split("=", limit = 2)
                        name to value
                    } else error("""Invalid config line $idx: "$line"""")
                }
    }
}
