package io.heapy.komok.tech.decorators.lib.annotations

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility

class KomokKCallable<R>(
    override val name: String,
    override val parameters: List<KParameter>,
    override val returnType: KType,
    override val typeParameters: List<KTypeParameter>,
    override val visibility: KVisibility,
    override val isFinal: Boolean,
    override val isOpen: Boolean,
    override val isAbstract: Boolean,
    override val isSuspend: Boolean,
    override val annotations: List<Annotation>
) : KCallable<R> {
    override fun call(vararg args: Any?): R {
        TODO("Not yet implemented")
    }

    override fun callBy(args: Map<KParameter, Any?>): R {
        TODO("Not yet implemented")
    }
}
