package io.heapy.komok.infra.http.server

import io.heapy.komok.server.common.KomokServerFeature
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

class CallLoggingFeature : KomokServerFeature {
    override fun Application.install() {
        install(CallLogging) {
            level = Level.INFO
            filter { call ->
                call.request
                    .path()
                    .startsWith("/")
            }
        }
    }
}
