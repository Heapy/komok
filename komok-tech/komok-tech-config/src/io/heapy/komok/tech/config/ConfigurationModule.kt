package io.heapy.komok.tech.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.heapy.komok.tech.config.dotenv.DotenvModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class ConfigurationModule(
    private val dotenvModule: DotenvModule,
) {
    context(_: KomokContext)
    open suspend fun config(): KomokConfiguration =
        HoconKomokConfiguration(hoconConfig())

    context(_: KomokContext)
    open suspend fun environmentOverrides(): Map<String, String> =
        mapOf()

    context(_: KomokContext)
    open suspend fun systemEnvironment(): Map<String, String> =
        System.getenv()

    context(_: KomokContext)
    open suspend fun effectiveEnvironment(): Map<String, String> =
        buildMap {
            putAll(systemEnvironment())
            putAll(dotenvModule.dotenv().properties)
            putAll(environmentOverrides())
        }

    context(_: KomokContext)
    open suspend fun hoconConfig(): Config {
        // Set system properties, so Hocon can read them and use as substitution variables
        effectiveEnvironment().forEach {
            System.setProperty(
                it.key,
                it.value
            )
        }

        return ConfigFactory.load()
    }
}
