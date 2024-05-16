package io.heapy.komok.business

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.event.Level

fun Application.configureMonitoring(
    meterRegistry: PrometheusMeterRegistry,
) {
    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    routing {
        get("/metrics-micrometer") {
            call.respond(meterRegistry.scrape())
        }
    }
}

