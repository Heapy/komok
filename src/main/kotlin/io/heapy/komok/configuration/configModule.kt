package io.heapy.komok.configuration

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.heapy.komok.infra.di.module
import io.heapy.komok.infra.di.provide

val configModule by module {
    provide(::config)
}

fun config(): Config = ConfigFactory.load()
