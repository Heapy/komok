package io.heapy.komok.tech.di.ez.impl

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.typeOf

@PublishedApi
internal inline fun <reified T> T.asProvider(): KFunction<T> {
    return object : KFunction<T> {
        override val annotations: List<Annotation> = emptyList()
        override val isAbstract: Boolean = false
        override val isExternal: Boolean = false
        override val isFinal: Boolean = true
        override val isInfix: Boolean = false
        override val isInline: Boolean = false
        override val isOpen: Boolean = false
        override val isOperator: Boolean = false
        override val isSuspend: Boolean = false
        override val name: String = "asInstance"
        override val parameters: List<KParameter> = emptyList()
        override val returnType: KType = typeOf<T>()
        override val typeParameters: List<KTypeParameter> = emptyList()
        override val visibility: KVisibility? = null
        override fun call(vararg args: Any?): T = this@asProvider
        override fun callBy(args: Map<KParameter, Any?>): T = this@asProvider
    }
}
