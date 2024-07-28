package io.heapy.komok.tech.di.test

import io.heapy.komok.tech.di.lib.Module

@Module
open class ApplicationModule(
    libraryModule: LibraryModule,
) : AutoCloseable {
    open val applicationService by lazy {
        ApplicationService(
            libraryService = libraryModule.libraryService,
        )
    }

    override fun close() {
        applicationService.close()
    }
}

@Module
open class LibraryModule {
    open val objectMapper by lazy {
        ObjectMapper(
            "objectMapper",
        )
    }

    open val libraryService by lazy {
        LibraryService(objectMapper, "libraryService-" + System.currentTimeMillis())
    }
}

class ObjectMapper(
    private val output: String,
) {
    fun map(input: String): String {
        return "$input -> $output"
    }
}

class LibraryService(
    private val objectMapper: ObjectMapper,
    private val instance: String,
) {
    fun call(): String {
        return objectMapper.map("Hello, $instance!")
    }
}

class ApplicationService(
    private val libraryService: LibraryService,
) : AutoCloseable {
    fun call() {
        println(libraryService.call())
    }

    override fun close() {
        println("ApplicationService closed")
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
