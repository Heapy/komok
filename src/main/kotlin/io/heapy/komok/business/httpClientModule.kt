package io.heapy.komok.business

import io.heapy.komok.infra.di.module
import io.heapy.komok.infra.di.provide
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.engine.cio.CIO

val httpClientModule by module {
    provide(::httpClient)
}

fun httpClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json = Json {
                ignoreUnknownKeys = true
            })
        }
    }
}

