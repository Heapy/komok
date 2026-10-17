package io.heapy.komok.infra.http.client

import io.heapy.komok.tech.di.lib.Module
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.engine.cio.CIO

@Module
open class HttpClientModule {
    open val httpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json = Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}
