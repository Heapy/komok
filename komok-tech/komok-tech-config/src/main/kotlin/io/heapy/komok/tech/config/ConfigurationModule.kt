package io.heapy.komok.tech.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.heapy.komok.tech.di.lib.Module
import io.heapy.komok.tech.config.dotenv.DotenvModule

@Module
open class ConfigurationModule(
    private val dotenvModule: DotenvModule,
) {
    open val config: KomokConfiguration by lazy {
        HoconKomokConfiguration(hoconConfig)
    }

    open val environmentOverrides: Map<String, String> by lazy {
        mapOf()
    }

    open val systemEnvironment: Map<String, String> by lazy {
        System.getenv()
    }

    open val effectiveEnvironment: Map<String, String> by lazy {
        buildMap {
            putAll(systemEnvironment)
            putAll(dotenvModule.dotenv.properties)
            putAll(environmentOverrides)
        }
    }

    open val hoconConfig: Config by lazy {
        // Set system properties, so Hocon can read them and use as substitution variables
        effectiveEnvironment.forEach {
            System.setProperty(
                it.key,
                it.value
            )
        }

        ConfigFactory.load()
    }
}
