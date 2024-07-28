package io.heapy.komok.metrics

import io.heapy.komok.tech.di.lib.Module
import io.micrometer.common.util.internal.logging.InternalLoggerFactory
import io.micrometer.common.util.internal.logging.Slf4JLoggerFactory
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun meterRegistry(): PrometheusMeterRegistry {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
    return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

@Module
open class MetricsModule {
    open val meterRegistry by lazy {
        meterRegistry()
    }
}
