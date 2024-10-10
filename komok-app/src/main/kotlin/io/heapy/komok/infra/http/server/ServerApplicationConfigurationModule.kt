package io.heapy.komok.infra.http.server

import io.heapy.komok.business.login.JwtModule
import io.heapy.komok.metrics.MetricsModule
import io.heapy.komok.server.common.KomokServerFeatures
import io.heapy.komok.tech.di.lib.Module

@Module
open class ServerApplicationConfigurationModule(
    private val metricsModule: MetricsModule,
    private val jwtModule: JwtModule,
) {
    open val features: KomokServerFeatures by lazy {
        KomokServerFeatures(
            features = listOf(
                webSocketsFeature,
                callLoggingFeature,
                monitoringFeature,
                defaultFeature,
            ),
        )
    }

    open val defaultFeature by lazy {
        DefaultFeature(jwtModule.config)
    }

    open val callLoggingFeature by lazy {
        CallLoggingFeature()
    }

    open val monitoringFeature by lazy {
        MonitoringFeature(metricsModule.meterRegistry)
    }

    open val webSocketsFeature by lazy {
        WebSocketsFeature()
    }
}
