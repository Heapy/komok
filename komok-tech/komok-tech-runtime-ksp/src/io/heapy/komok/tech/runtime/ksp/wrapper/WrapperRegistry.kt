package io.heapy.komok.tech.runtime.ksp.wrapper

import com.google.devtools.ksp.processing.KSPLogger
import java.util.ServiceLoader

/**
 * Wrapper plugins discovered on the processor classpath, held in deterministic chain
 * order (ascending [WrapperPlugin.order], then annotation FQN). A duplicate claim on the
 * same annotation is reported as an error and the later plugin is dropped.
 */
internal class WrapperRegistry private constructor(
    val plugins: List<WrapperPlugin>,
) {
    companion object {
        fun load(logger: KSPLogger): WrapperRegistry {
            val discovered = ServiceLoader
                .load(WrapperPlugin::class.java, WrapperPlugin::class.java.classLoader)
                .sortedWith(compareBy({ it.order }, { it.annotationFqn }))

            val byAnnotation = LinkedHashMap<String, WrapperPlugin>()
            discovered.forEach { plugin ->
                val existing = byAnnotation.putIfAbsent(plugin.annotationFqn, plugin)
                if (existing != null) {
                    logger.error(
                        "Multiple WrapperPlugins claim ${plugin.annotationFqn}: " +
                            "${existing::class.qualifiedName} and ${plugin::class.qualifiedName}.",
                    )
                }
            }
            return WrapperRegistry(byAnnotation.values.toList())
        }
    }
}
