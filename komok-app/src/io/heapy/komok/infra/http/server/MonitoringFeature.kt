package io.heapy.komok.infra.http.server

import io.heapy.komok.server.common.KomokServerFeature
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class MonitoringFeature(
    private val meterRegistry: PrometheusMeterRegistry,
) : KomokServerFeature {
    override fun Application.install() {
        install(MicrometerMetrics) {
            registry = meterRegistry
        }

        routing {
            get("/metrics") {
                call.respond(meterRegistry.scrape())
            }
        }
    }
}
