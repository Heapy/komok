package io.heapy.komok.tech.decorators.plugins.logging

import io.heapy.komok.tech.decorators.ksp.api.DecoratorPlugin
import io.heapy.komok.tech.decorators.plugins.logging.annotations.Log
import kotlin.reflect.KClass

class LoggingDecoratorPlugin : DecoratorPlugin {
    override fun getAnnotationTypes(): List<KClass<out Annotation>> {
        return listOf(Log::class)
    }

    override fun getWrappingFunctionFqn(paramCount: Int): String {
        return "io.heapy.komok.tech.decorators.plugins.logging.log$paramCount"
    }
}
