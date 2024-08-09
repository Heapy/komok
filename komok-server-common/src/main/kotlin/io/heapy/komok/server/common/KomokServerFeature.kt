package io.heapy.komok.server.common

import io.ktor.server.application.Application

interface KomokServerFeature {
    fun Application.install()
}
