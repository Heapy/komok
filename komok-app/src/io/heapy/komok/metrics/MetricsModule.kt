package io.heapy.komok.metrics

import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import io.micrometer.common.util.internal.logging.InternalLoggerFactory
import io.micrometer.common.util.internal.logging.Slf4JLoggerFactory
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

@Module
class MetricsModule {
    context(_: KotomiContext)
    suspend fun meterRegistry(): PrometheusMeterRegistry {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }
}
