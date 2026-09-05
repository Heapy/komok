package io.heapy.komok.tech.runtime.ksp.wrapper

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec

/**
 * SPI for produced-value method-call wrappers (`@Log`-style). The processor discovers
 * implementations with `java.util.ServiceLoader` on the KSP processor classpath
 * (`META-INF/services/io.heapy.komok.tech.runtime.ksp.wrapper.WrapperPlugin`), so a new
 * wrapper kind ships as an ordinary library pair — annotation + runtime helper on the
 * compile classpath, plugin jar on the `ksp(...)` configuration — with **no processor
 * changes**. The built-in `@Log`/`@Metric`/`@Retry` wrappers register through the same
 * mechanism and have no special status in the core.
 *
 * Contract: the runtime helper has the shape
 * `context(C)? suspend fun <T> helper(...config..., call: suspend () -> T): T`. The core
 * generates the deterministic nesting `outer(...) { inner(...) { delegate.m(...) } }`
 * ordered by ascending [order], and emits the [contextParameter]s of all applied
 * wrappers (same order, deduplicated) on the decorator override — the wrapped interface
 * method must declare exactly those context parameters, caller-provided at the call
 * site.
 */
interface WrapperPlugin {
    /** FQN of the annotation (method- or type-level) that applies this wrapper. */
    val annotationFqn: String

    /** Chain position, ascending = outermost first. Built-ins: `@Log` 100, `@Metric` 200, `@Retry` 300. */
    val order: Int

    /** Context parameter type the wrapped method must declare for this wrapper, if any. */
    val contextParameter: ClassName?
        get() = null

    /** Private properties the decorator class needs (deduplicated by name across wrappers). */
    fun decoratorProperties(type: WrappedTypeInfo): List<PropertySpec> = emptyList()

    /** Config validation; returned messages are reported as KSP errors on the method. */
    fun validate(method: KSFunctionDeclaration, annotation: KSAnnotation): List<String> = emptyList()

    /** The helper invocation WITHOUT the trailing lambda — the core appends `{ <inner> }`. */
    fun helperCall(site: WrapperSite): CodeBlock
}

/** The decorated service type, as seen by [WrapperPlugin.decoratorProperties]. */
class WrappedTypeInfo(
    val className: ClassName,
    val fqn: String,
)

/** One wrapped-method occurrence handed to [WrapperPlugin.helperCall]. */
class WrapperSite(
    val annotation: KSAnnotation,
    val method: KSFunctionDeclaration,
    /** Reference to the generated static `MethodMeta`, e.g. `GreeterMeta.greet`. */
    val metaReference: CodeBlock,
)

/** Annotation argument by name — explicit value or declared default. */
fun KSAnnotation.argument(name: String): Any? =
    arguments.firstOrNull { it.name?.asString() == name }?.value
        ?: defaultArguments.firstOrNull { it.name?.asString() == name }?.value

fun KSAnnotation.booleanArgument(name: String): Boolean? = argument(name) as? Boolean

fun KSAnnotation.stringArgument(name: String): String? = argument(name) as? String

fun KSAnnotation.intArgument(name: String): Int? = (argument(name) as? Number)?.toInt()

fun KSAnnotation.longArgument(name: String): Long? = (argument(name) as? Number)?.toLong()

/** Simple name of an enum-typed argument (e.g. `DEBUG` for `Level.DEBUG`). */
fun KSAnnotation.enumArgumentName(name: String): String? =
    when (val value = argument(name)) {
        is KSType -> value.declaration.simpleName.asString()
        is KSClassDeclaration -> value.simpleName.asString()
        else -> null
    }
