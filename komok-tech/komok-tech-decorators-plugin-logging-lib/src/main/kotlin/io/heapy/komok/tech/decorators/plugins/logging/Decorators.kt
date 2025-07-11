package io.heapy.komok.tech.decorators.plugins.logging

import io.heapy.komok.tech.decorators.plugins.logging.annotations.Log
import org.slf4j.Logger
import org.slf4j.spi.LoggingEventBuilder
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.time.measureTimedValue

@PublishedApi
context(log: Logger, annotation: Log)
internal inline fun <T> runAndLog(
    methodName: String,
    atLevel: LoggingEventBuilder,
    call: () -> T,
): T {
    return try {
        if (annotation.includeExecutionTime) {
            val (result, duration) = measureTimedValue { call() }
            if (annotation.includeResult) {
                atLevel.log(
                    "{} returned: {}, took {}ms",
                    methodName,
                    result,
                    duration.inWholeMilliseconds,
                )
            } else {
                atLevel.log(
                    "{} returned, took {}ms",
                    methodName,
                    duration.inWholeMilliseconds,
                )
            }
            result
        } else {
            val result = call()
            if (annotation.includeResult) {
                atLevel.log(
                    "{} returned: {}",
                    methodName,
                    result,
                )
            } else {
                atLevel.log(
                    "{} returned",
                    methodName,
                )
            }
            result
        }
    } catch (e: Exception) {
        if (annotation.includeStacktrace) {
            log.error(
                "{} failed",
                methodName,
                e,
            )
        } else {
            log.error(
                "{} failed: {}",
                methodName,
                e.message,
            )
        }
        throw e
    }
}

context(log: Logger, function: KCallable<*>, annotation: Log)
inline fun <R> log0(
    block: () -> R,
): R {
    val methodName = function.name
    val atLevel = log.atLevel(annotation.level)
    atLevel.log(
        "Calling {}()",
        methodName,
    )
    return runAndLog(
        methodName,
        atLevel,
    ) { block() }
}

context(log: Logger, function: KCallable<*>, annotation: Log)
inline fun <P1, R> log1(
    p1: P1,
    block: (P1) -> R,
): R {
    val methodName = function.name
    val atLevel = log.atLevel(annotation.level)
    if (annotation.includeParameters) {
        atLevel.log {
            val p1Name = function.parameters[0].name
            "Calling ${methodName}($p1Name: $p1)"
        }
    } else {
        atLevel.log {
            val p1Name = function.parameters[0].name
            "Calling ${methodName}(${p1Name})"
        }
    }
    return runAndLog(
        methodName,
        atLevel,
    ) { block(p1) }
}

context(log: Logger, function: KFunction<*>, annotation: Log)
inline fun <P1, P2, R> log2(
    p1: P1,
    p2: P2,
    block: (P1, P2) -> R,
): R {
    val methodName = function.name
    val atLevel = log.atLevel(annotation.level)
    if (annotation.includeParameters) {
        atLevel.log {
            val p1Name = function.parameters[0].name
            val p2Name = function.parameters[1].name
            "Calling ${methodName}($p1Name: $p1, $p2Name: $p2)"
        }
    } else {
        atLevel.log {
            val p1Name = function.parameters[0].name
            val p2Name = function.parameters[1].name
            "Calling ${methodName}(${p1Name}, ${p2Name})"
        }
    }
    return runAndLog(
        methodName,
        atLevel,
    ) {
        block(
            p1,
            p2,
        )
    }
}
