package io.heapy.komok.configuration

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.heapy.komok.tech.di.lib.Module

@Module
open class ConfigurationModule {
    open val config: Config by lazy {
        ConfigFactory.load()
    }
}
