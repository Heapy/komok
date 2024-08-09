package io.heapy.komok.configuration

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.heapy.komok.tech.di.lib.Module

@Module
open class ConfigModule {
    open val config: KomokConfiguration by lazy {
        HoconKomokConfiguration(hoconConfig)
    }

    internal val hoconConfig: Config by lazy {
        ConfigFactory.load()
    }
}
