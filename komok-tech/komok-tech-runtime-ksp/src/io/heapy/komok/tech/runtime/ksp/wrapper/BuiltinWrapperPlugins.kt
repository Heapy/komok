package io.heapy.komok.tech.runtime.ksp.wrapper

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec

/**
 * The built-in wrappers, registered through the same ServiceLoader SPI as third-party
 * ones — the core processor has no knowledge of them beyond this file's
 * `META-INF/services` entry.
 */

private const val WRAPPERS = "io.heapy.komok.tech.runtime.wrappers"
private const val LOGGING = "io.heapy.komok.tech.logging"
private const val MICROMETER = "io.micrometer.core.instrument"

/** `@Log` — `context(_: LoggingContext)`; the logger *name* is per-type static identity. */
class LogWrapperPlugin : WrapperPlugin {
    override val annotationFqn: String = "$WRAPPERS.Log"
    override val order: Int = 100
    override val contextParameter: ClassName = ClassName(LOGGING, "LoggingContext")

    override fun decoratorProperties(type: WrappedTypeInfo): List<PropertySpec> =
        listOf(
            PropertySpec.builder("logger", ClassName(LOGGING, "Logger"), KModifier.PRIVATE)
                .initializer("%M(%S)", MemberName(LOGGING, "logger"), type.fqn)
                .build(),
        )

    override fun helperCall(site: WrapperSite): CodeBlock =
        CodeBlock.of(
            "%M(logger, %L, %T.%N, %L, %L)",
            MemberName(WRAPPERS, "logCall"),
            site.metaReference,
            ClassName(LOGGING, "Level"),
            site.annotation.enumArgumentName("level") ?: "DEBUG",
            site.annotation.booleanArgument("includeResult") ?: true,
            site.annotation.booleanArgument("includeDuration") ?: true,
        )
}

/** `@Metric` — `context(_: MeterRegistry)`. */
class MetricWrapperPlugin : WrapperPlugin {
    override val annotationFqn: String = "$WRAPPERS.Metric"
    override val order: Int = 200
    override val contextParameter: ClassName = ClassName(MICROMETER, "MeterRegistry")

    override fun helperCall(site: WrapperSite): CodeBlock =
        CodeBlock.of(
            "%M(%L, %S)",
            MemberName(WRAPPERS, "metricCall"),
            site.metaReference,
            site.annotation.stringArgument("name") ?: "",
        )
}

/** `@Retry` — no context. */
class RetryWrapperPlugin : WrapperPlugin {
    override val annotationFqn: String = "$WRAPPERS.Retry"
    override val order: Int = 300

    override fun helperCall(site: WrapperSite): CodeBlock =
        CodeBlock.of(
            "%M(%L, %L, %LL)",
            MemberName(WRAPPERS, "retryCall"),
            site.metaReference,
            site.annotation.intArgument("maxAttempts") ?: 3,
            site.annotation.longArgument("delayMillis") ?: 0L,
        )
}
