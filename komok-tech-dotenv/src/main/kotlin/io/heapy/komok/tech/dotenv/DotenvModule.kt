package io.heapy.komok.tech.dotenv

import io.heapy.komok.tech.di.lib.Module

@Module
open class DotenvModule {
    open val dotenv: Map<String, String> by lazy {
        dotenv()
    }
}
