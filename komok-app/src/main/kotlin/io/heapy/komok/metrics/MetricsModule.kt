package io.heapy.komok.metrics

import io.heapy.komok.tech.di.lib.Module
import io.micrometer.common.util.internal.logging.InternalLoggerFactory
import io.micrometer.common.util.internal.logging.Slf4JLoggerFactory
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

@Module
open class MetricsModule {
    open val meterRegistry: PrometheusMeterRegistry by lazy {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }
}
