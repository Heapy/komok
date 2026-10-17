package io.heapy.komok.tech.config.dotenv

import io.heapy.komok.tech.config.common.Environment
import io.heapy.komok.tech.di.lib.Module

@Module
open class DotenvModule {
    open val dotenv: Environment by lazy {
        dotenv()
    }
}
