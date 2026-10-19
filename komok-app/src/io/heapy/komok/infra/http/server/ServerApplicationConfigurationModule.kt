package io.heapy.komok.infra.http.server

import io.heapy.komok.infra.jwt.JwtModule
import io.heapy.komok.metrics.MetricsModule
import io.heapy.komok.server.common.KomokServerFeatures
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class ServerApplicationConfigurationModule(
    private val metricsModule: MetricsModule,
    private val jwtModule: JwtModule,
) {
    context(_: KomokContext)
    suspend fun features(): KomokServerFeatures =
        KomokServerFeatures(
            features = listOf(
                webSocketsFeature(),
                callLoggingFeature(),
                monitoringFeature(),
                defaultFeature(),
            ),
        )

    context(_: KomokContext)
    suspend fun defaultFeature(): DefaultFeature =
        DefaultFeature(jwtModule.jwtConfiguration())

    context(_: KomokContext)
    suspend fun callLoggingFeature(): CallLoggingFeature =
        CallLoggingFeature()

    context(_: KomokContext)
    suspend fun monitoringFeature(): MonitoringFeature =
        MonitoringFeature(metricsModule.meterRegistry())

    context(_: KomokContext)
    suspend fun webSocketsFeature(): WebSocketsFeature =
        WebSocketsFeature()
}
