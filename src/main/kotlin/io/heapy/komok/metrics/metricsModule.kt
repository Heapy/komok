package io.heapy.komok.metrics

import io.heapy.komok.infra.di.module
import io.heapy.komok.infra.di.provide
import io.micrometer.common.util.internal.logging.InternalLoggerFactory
import io.micrometer.common.util.internal.logging.Slf4JLoggerFactory
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val metricsModule by module {
    provide(::meterRegistry)
}

fun meterRegistry(): PrometheusMeterRegistry {
    InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
    return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
