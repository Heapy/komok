package io.heapy.komok.infra.http.server

import io.heapy.komok.infra.jwt.JwtModule
import io.heapy.komok.metrics.MetricsModule
import io.heapy.komok.server.common.KomokServerFeatures
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class ServerApplicationConfigurationModule(
    private val metricsModule: MetricsModule,
    private val jwtModule: JwtModule,
) {
    context(_: KotomiContext)
    suspend fun features(): KomokServerFeatures =
        KomokServerFeatures(
            features = listOf(
                webSocketsFeature(),
                callLoggingFeature(),
                monitoringFeature(),
                defaultFeature(),
            ),
        )

    context(_: KotomiContext)
    suspend fun defaultFeature(): DefaultFeature =
        DefaultFeature(jwtModule.jwtConfiguration())

    context(_: KotomiContext)
    suspend fun callLoggingFeature(): CallLoggingFeature =
        CallLoggingFeature()

    context(_: KotomiContext)
    suspend fun monitoringFeature(): MonitoringFeature =
        MonitoringFeature(metricsModule.meterRegistry())

    context(_: KotomiContext)
    suspend fun webSocketsFeature(): WebSocketsFeature =
        WebSocketsFeature()
}
