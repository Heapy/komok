package io.heapy.komok.tech.di.test

import io.heapy.komok.tech.di.lib.Module

@Module
open class DependentModule(
    private val applicationModule: ApplicationModule,
) {
    open val dependentServer by lazy {
        DependentServer(applicationModule.applicationService)
    }
}

class DependentServer(
    private val applicationService: ApplicationService,
) {
    fun bar() {
        applicationService.call()
    }
}

fun main() {
    val applicationModule = createApplicationModule {
        libraryModule {
            objectMapper {
                ObjectMapper("foo")
            }
        }
    }

    applicationModule.applicationService.call()
}
