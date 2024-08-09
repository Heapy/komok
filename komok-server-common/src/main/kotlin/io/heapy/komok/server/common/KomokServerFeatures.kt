package io.heapy.komok.server.common

import io.ktor.server.application.Application

class KomokServerFeatures(
    private val features: List<KomokServerFeature>,
) : KomokServerFeature {
    override fun Application.install() {
        features.forEach { feature ->
            feature.run {
                install()
            }
        }
    }
}
