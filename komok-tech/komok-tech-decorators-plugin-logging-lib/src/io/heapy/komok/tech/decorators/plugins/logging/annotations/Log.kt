package io.heapy.komok.tech.decorators.plugins.logging.annotations

import org.slf4j.event.Level

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Log(
    val level: Level = Level.DEBUG,
    val includeParameters: Boolean = true,
    val includeResult: Boolean = true,
    val includeStacktrace: Boolean = true,
    val includeExecutionTime: Boolean = true,
)
